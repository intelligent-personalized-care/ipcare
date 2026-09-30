/*
 * Dual-IMU Joint Position Detection
 *
 * Hardware:
 * - ESP32-C3 Super Mini
 * - ICM-20948 proximal segment: address 0x68
 * - ICM-20948 distal segment: address 0x69 (same wired I2C bus)
 * - TTP223 capacitive touch input (active HIGH, momentary mode)
 * - Three-channel RGB LED (PWM outputs)
 * - BLE GATT communication
 */

#include <Wire.h>
#include <atomic>
#include <math.h>
#include "ExerciseProtocol.h"
#include "ExerciseOrientation.h"
#include <ICM20948_WE.h>

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

// ======================================================
// PIN CONFIGURATION
// ======================================================

#define SDA_PIN 8
#define SCL_PIN 9

#define BUTTON_PIN 0

#define RED_PIN   10
#define GREEN_PIN 3
#define BLUE_PIN  1

// TTP223 OUT connects to GPIO0; HIGH while touched, LOW when released.
// Use momentary (non-toggle), active-HIGH operation.

// ======================================================
// IMU CONFIGURATION
// ======================================================

#define IMU_PROXIMAL_ADDR 0x68  // AD0 -> GND
#define IMU_DISTAL_ADDR   0x69  // AD0 -> 3V3

#define SERIAL_BAUD 115200
#define DEBUG_SENSOR_STREAM 0
#define SENSOR_READ_INTERVAL_MS 50
#define CALIBRATION_SAMPLES 100
#define FILTER_SIZE 5

ICM20948_WE imuProximal(IMU_PROXIMAL_ADDR);
ICM20948_WE imuDistal(IMU_DISTAL_ADDR);

// ======================================================
// BLE CONFIGURATION
// ======================================================

#define BLE_SERVICE_UUID \
  "12345678-1234-1234-1234-1234567890ab"

#define BLE_CHARACTERISTIC_UUID \
  "abcd1234-5678-1234-5678-abcdef123456"

BLEServer *bleServer = nullptr;
BLECharacteristic *bleCharacteristic = nullptr;

std::atomic<bool> bleClientConnected{false};
std::atomic<bool> connectionChanged{false};
std::atomic<uint32_t> connectionGeneration{0};
std::atomic<bool> commandRejected{false};
struct QueuedCommand { uint32_t generation; char text[WearableProtocol::MAX_COMMAND_BYTES + 1]; };
BLE2902 *notificationDescriptor = nullptr;
bool sensorsReady = false;
bool sensorFault = false;
bool motionWarning = false;
bool calibrationActive = false;
unsigned long calibrationStarted = 0, calibrationSampleAt = 0;
int calibrationCount = 0;
float calibrationSin[4] = {}, calibrationCos[4] = {};
float calibrationFirst[4] = {};
unsigned long lastRecoveryAttempt = 0;
uint32_t processedGeneration = 0;
bool subscriptionReported = false;
QueueHandle_t commandQueue = nullptr;
bool sessionActive = false;
int targetRepetitions = 0;
bool startPositionSeen = false;
float peakRaiseSpeed = 0.0f;
float previousSpeedAngle = 0.0f;
unsigned long previousSpeedTime = 0;

// ======================================================
// RGB LED
// ======================================================

enum LedMode {
  LED_OFF,
  LED_BOOTING,
  LED_ADVERTISING,
  LED_CONNECTED,
  LED_CALIBRATING,
  LED_ACTIVE,
  LED_SUCCESS,
  LED_WARNING
};

LedMode ledMode = LED_BOOTING;

unsigned long ledPreviousMillis = 0;
bool ledBlinkState = false;
unsigned long temporaryLedUntil = 0;
LedMode previousLedMode = LED_ACTIVE;


// ======================================================
// EXERCISE CONFIGURATION
// ======================================================

struct JointLimits {
  float minRelativePitch;
  float maxRelativePitch;
  float minRelativeRoll;
  float maxRelativeRoll;
};

struct Exercise {
  String name;

  // true: relative roll is the primary exercise angle
  // false: relative pitch is the primary exercise angle
  bool useRoll;
  int direction;

  float raisedThreshold;
  float loweredThreshold;

  unsigned long minimumRaiseTimeMs;
  unsigned long holdTimeMs;
  unsigned long cooldownMs;

  float minimumMovementSpeed;

  JointLimits limits;
};

Exercise currentExercise = {
  "Not configured",

  false,
  1,

  0.0f,
  0.0f,

  0,
  0,
  0,

  0.0f,

  {
    -180.0f,
     180.0f,
    -180.0f,
     180.0f
  }
};

bool exerciseConfigured = false;
int configuredRepetitions = 0;

// ======================================================
// MOVEMENT STATE
// ======================================================

enum MovementState {
  WAITING_FOR_START,
  RAISING,
  HOLDING,
  RETURNING
};

MovementState movementState = WAITING_FOR_START;

unsigned long raiseStartTime = 0;
unsigned long holdStartTime = 0;
unsigned long lastRepTime = 0;
unsigned long lastSensorReadTime = 0;

int repetitionCount = 0;
int invalidRepetitionCount = 0;

// ======================================================
// CALIBRATION
// ======================================================

// Relative orientation measured in the initial neutral posture.
float relativePitchZero = 0.0f;
float relativeRollZero = 0.0f;

bool sensorsCalibrated = false;

// ======================================================
// FILTERS
// ======================================================

struct MovingAverageFilter {
  float values[FILTER_SIZE];
  int index;
  bool initialized;
};

MovingAverageFilter relativePitchFilter = {{0}, 0, false};
MovingAverageFilter relativeRollFilter = {{0}, 0, false};

// ======================================================
// BUTTON
// ======================================================

enum ButtonAction {
  BUTTON_NO_ACTION,
  BUTTON_SHORT_PRESS,
  BUTTON_MEDIUM_PRESS,
  BUTTON_LONG_PRESS
};

bool previousButtonPressed = false;
bool rawButtonPressed = false;
bool buttonArmed = false;
unsigned long buttonPressedAt = 0;
unsigned long lastButtonChangeAt = 0;

const unsigned long BUTTON_DEBOUNCE_MS = 40;
const unsigned long MEDIUM_PRESS_MS = 1500;
const unsigned long LONG_PRESS_MS = 4000;

void setLedMode(LedMode mode);
void updateLed();
void showTemporaryLed(LedMode temporaryMode, unsigned long durationMs);

// ======================================================
// BLE CALLBACKS
// ======================================================

class BleServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override {
    (void)server;

    connectionGeneration.fetch_add(1);
    bleClientConnected = true;
    connectionChanged = true;

    Serial.println("BLE client connected.");
  }

  void onDisconnect(BLEServer *server) override {
    (void)server;

    connectionGeneration.fetch_add(1);
    bleClientConnected = false;

    connectionChanged = true;

    Serial.println("BLE client disconnected. Advertising restarted.");
  }
};

class SensorCommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *characteristic) override {
    auto value = characteristic->getValue();
    if (value.length() == 0 || value.length() > WearableProtocol::MAX_COMMAND_BYTES || commandQueue == nullptr ||
        memchr(value.c_str(), 0, value.length()) != nullptr) {
      commandRejected = true;
      return;
    }
    QueuedCommand command{};
    command.generation = connectionGeneration.load();
    memcpy(command.text, value.c_str(), value.length());
    if (xQueueSend(commandQueue, &command, 0) != pdTRUE) commandRejected = true;


  }
};

// ======================================================
// SETUP
// ======================================================

void setup() {
  Serial.begin(SERIAL_BAUD);
  delay(1000);

  initializeButton();
  initializeLed();

  setLedMode(LED_BOOTING);

  Serial.println();
  Serial.println("====================================");
  Serial.println(" DUAL-IMU JOINT MONITORING SYSTEM");
  Serial.println("====================================");

  Wire.begin(SDA_PIN, SCL_PIN);

  // A lower frequency is safer with the wired remote IMU.
  Wire.setClock(100000);

  scanI2CBus();
  Wire.setTimeOut(25);
  initializeBle(); // Remain discoverable even when a module is absent.
  sensorsReady = initializeSensors();
  sensorFault = !sensorsReady;
  sensorsCalibrated = false; // Never calibrate an unknown boot posture automatically.
  setLedMode(sensorFault ? LED_WARNING : LED_ADVERTISING);

  Serial.println("System ready.");
  Serial.println("Active exercise: " + currentExercise.name);
  printButtonInstructions();
}

// ======================================================
// MAIN LOOP
// ======================================================

void loop() {
  if (connectionChanged.exchange(false)) {
    processedGeneration = connectionGeneration.load();
    resetExerciseSession();
    calibrationActive = false;
    sensorsCalibrated = false;
    subscriptionReported = false;
    exerciseConfigured = false; configuredRepetitions = 0;
    temporaryLedUntil = 0;
    // Clear CCCD only on disconnect: Android may already have subscribed when
    // the main loop observes a connection callback.
    if (!bleClientConnected && notificationDescriptor) notificationDescriptor->setNotifications(false);
    if (!bleClientConnected) BLEDevice::startAdvertising();
  }
  const bool subscribed = bleClientConnected && notificationDescriptor && notificationDescriptor->getNotifications();
  if (subscribed && !subscriptionReported) {
    subscriptionReported = true;
    notifyBleMessage(sensorFault ? "ERROR:SENSOR MISSING" : "CALIBRATION REQUIRED");
  }
  if (subscriptionReported && !subscribed && sessionActive) {
    resetExerciseSession();
    sensorsCalibrated = false;
    subscriptionReported = false;
  }
  if (commandRejected.exchange(false)) {
    resetExerciseSession();
    calibrationActive = false;
    notifyBleMessage("ERROR:COMMAND REJECTED");
  }
  QueuedCommand command{};
  if (commandQueue && xQueueReceive(commandQueue, &command, 0) == pdTRUE &&
      command.generation == processedGeneration && command.generation == connectionGeneration.load() && bleClientConnected) {
    String rawCommand = String(command.text);

  int separatorIndex = rawCommand.indexOf(':');

  String commandName;
  String payload;

  if (separatorIndex >= 0) {
    commandName = rawCommand.substring(0, separatorIndex);
    payload = rawCommand.substring(separatorIndex + 1);
  } else {
    commandName = rawCommand;
    payload = "";
  }

  commandName.trim();
  payload.trim();

  Serial.println("BLE command: " + commandName);
  Serial.println("BLE payload: " + payload);

  handleCommand(commandName, payload);
  }
  ButtonAction action = readButtonAction();
  if (action != BUTTON_NO_ACTION) handleButtonAction(action);
  updateCalibration();
  const unsigned long now = millis();
  if (!sensorsReady && !calibrationActive && now - lastRecoveryAttempt >= 2000) {
    lastRecoveryAttempt = now;
    if (sensorsPresent()) {
      sensorsReady = initializeSensors();
      sensorFault = !sensorsReady;
      if (sensorsReady) notifyBleMessage("CALIBRATION REQUIRED");
    }
  }
  updateLed();

  // A slower, uncounted preview lets the user verify axis/sign before START2.
  // Calibration ACK gets a quiet interval before the first preview packet.
  const unsigned long sampleInterval = sessionActive ? SENSOR_READ_INTERVAL_MS : 250;
  if (!calibrationActive && sensorsCalibrated && bleClientConnected &&
      now - lastSensorReadTime >= sampleInterval) {
    lastSensorReadTime = now;
    readAndProcessSensors();
  }
  delay(2); // Yield to BLE and the watchdog without delaying button handling.
}

bool sensorsPresent() {
  Wire.beginTransmission(IMU_PROXIMAL_ADDR);
  bool proximal = Wire.endTransmission() == 0;
  Wire.beginTransmission(IMU_DISTAL_ADDR);
  return Wire.endTransmission() == 0 && proximal;
}

void failSensors(const char *message) {
  resetExerciseSession();
  calibrationActive = false;
  sensorsCalibrated = false;
  sensorsReady = false;
  sensorFault = true;
  temporaryLedUntil = 0;
  notifyBleMessage(message);
}


// ======================================================
// IMU INITIALIZATION
// ======================================================

bool initializeSensors() {
  Serial.println("Initializing proximal IMU at 0x68...");

  if (!imuProximal.init()) {
    Serial.println("ERROR: proximal IMU was not found at 0x68.");
    return false;
  }

  Serial.println("Proximal IMU found.");

  Serial.println("Initializing distal IMU at 0x69...");

  if (!imuDistal.init()) {
    Serial.println("ERROR: distal IMU was not found at 0x69.");
    return false;
  }

  Serial.println("Distal IMU found.");

  configureImu(imuProximal);
  configureImu(imuDistal);

  return true;
}

void configureImu(ICM20948_WE &imu) {
  imu.setAccRange(ICM20948_ACC_RANGE_2G);
  imu.setAccDLPF(ICM20948_DLPF_6);

  imu.setGyrDLPF(ICM20948_DLPF_6);

  imu.setGyrRange(ICM20948_GYRO_RANGE_250);
}

// ======================================================
// SENSOR CALIBRATION
// ======================================================

void calibrateBothSensors() {
  Serial.println(exerciseConfigured);
  if (!exerciseConfigured) { notifyBleMessage("ERROR:PROFILE REQUIRED"); return; }
  resetExerciseSession();
  sensorsCalibrated = false;
  // Reinitialize both IMUs on every attempt, including TTP223 recalibration.
  if (!sensorsPresent() || !initializeSensors()) {
    failSensors("ERROR:SENSOR MISSING");
    return;
  }
  sensorsReady = true;
  sensorFault = false;
  resetFilters();
  relativePitchZero = relativeRollZero = 0;
  memset(calibrationFirst, 0, sizeof(calibrationFirst));
  calibrationActive = true;
  calibrationStarted = millis();
  calibrationSampleAt = millis();
  calibrationCount = 0;
  memset(calibrationSin, 0, sizeof(calibrationSin));
  memset(calibrationCos, 0, sizeof(calibrationCos));
  temporaryLedUntil = 0;
  notifyBleMessage("CALIBRATING");
}

void updateCalibration() {
  if (!calibrationActive) return;
  const unsigned long now = millis();
  if (now - calibrationStarted >= 12000) {
    calibrationActive = false;
    showTemporaryLed(LED_WARNING, 1500);
    notifyBleMessage("ERROR:CALIBRATION MOVED");
    return;
  }
  if (now - calibrationSampleAt < 20) return;
  calibrationSampleAt = now;
  if (!sensorsPresent()) { failSensors("ERROR:SENSOR MISSING"); return; }
  imuProximal.readSensor();
  imuDistal.readSensor();
  float angles[4] = {imuProximal.getPitch(), imuProximal.getRoll(), imuDistal.getPitch(), imuDistal.getRoll()};

  // Read and discard fresh samples during settling; never use a stale previous pose.
  if (now - calibrationStarted < 1000) return;

  // Validate the complete sample before updating any accumulator.
  bool moved = false;
  for (int i = 0; i < 4; ++i) {
    if (!isfinite(angles[i])) { failSensors("ERROR:INVALID SENSOR DATA"); return; }
    if (calibrationCount > 0 &&
        fabsf(normalizeAngle(angles[i] - calibrationFirst[i])) > 6.0f) moved = true;
  }
  if (moved) {
    // Search for a new stable window instead of failing on one noisy sample.
    calibrationCount = 0;
    memset(calibrationSin, 0, sizeof(calibrationSin));
    memset(calibrationCos, 0, sizeof(calibrationCos));
  }
  for (int i = 0; i < 4; ++i) {
    if (calibrationCount == 0) calibrationFirst[i] = angles[i];
    calibrationSin[i] += sinf(angles[i] * DEG_TO_RAD);
    calibrationCos[i] += cosf(angles[i] * DEG_TO_RAD);
  }
  if (++calibrationCount < CALIBRATION_SAMPLES) return;
  float mean[4];
  for (int i = 0; i < 4; ++i) mean[i] = atan2f(calibrationSin[i], calibrationCos[i]) * RAD_TO_DEG;
  relativePitchZero = normalizeAngle(mean[2] - mean[0]);
  relativeRollZero = normalizeAngle(mean[3] - mean[1]);
  resetFilters();
  previousSpeedTime = 0;
  calibrationActive = false;
  sensorsCalibrated = true;
  lastSensorReadTime = millis();

  Serial.println("CALIBRATION COMPLETE");
  Serial.print("Zero relative pitch: ");
  Serial.println(relativePitchZero, 2);
  Serial.print("Zero relative roll: ");
  Serial.println(relativeRollZero, 2);

  // Send the completion acknowledgement before any exercise telemetry can start.
  notifyBleMessage("CALIBRATION COMPLETE");
  showTemporaryLed(LED_SUCCESS, 1200);
}


// ======================================================
// SENSOR PROCESSING
// ======================================================

void readAndProcessSensors() {
  if (!sensorsCalibrated) {
    return;
  }

  Wire.beginTransmission(IMU_PROXIMAL_ADDR);
  const bool proximalPresent = Wire.endTransmission() == 0;
  Wire.beginTransmission(IMU_DISTAL_ADDR);
  const bool distalPresent = Wire.endTransmission() == 0;
  if (!proximalPresent || !distalPresent) {
    failSensors("ERROR:SENSOR MISSING");
    return;
  }

  imuProximal.readSensor();
  imuDistal.readSensor();

  float proximalPitch = imuProximal.getPitch();
  float proximalRoll = imuProximal.getRoll();

  float distalPitch = imuDistal.getPitch();
  float distalRoll = imuDistal.getRoll();

  if (!isfinite(proximalPitch) || !isfinite(proximalRoll) || !isfinite(distalPitch) || !isfinite(distalRoll)) {
    failSensors("ERROR:INVALID SENSOR DATA");
    return;
  }

  float relativePitchRaw = normalizeAngle(
    distalPitch - proximalPitch - relativePitchZero
  );

  float relativeRollRaw = normalizeAngle(
    distalRoll - proximalRoll - relativeRollZero
  );

  float relativePitch = applyMovingAverage(
    relativePitchFilter,
    relativePitchRaw
  );

  float relativeRoll = applyMovingAverage(
    relativeRollFilter,
    relativeRollRaw
  );

  const float primaryAngle = ExerciseOrientation::orient(
    relativePitch, relativeRoll, currentExercise.useRoll, currentExercise.direction).primary;

  float movementSpeed = calculateMovementSpeed(primaryAngle);

  processExercise(
    proximalPitch,
    proximalRoll,
    distalPitch,
    distalRoll,
    relativePitch,
    relativeRoll,
    primaryAngle,
    movementSpeed
  );
}

// ======================================================
// EXERCISE STATE MACHINE
// ======================================================

void processExercise(
  float proximalPitch,
  float proximalRoll,
  float distalPitch,
  float distalRoll,
  float relativePitch,
  float relativeRoll,
  float primaryAngle,
  float movementSpeed
) {
  const unsigned long now = millis();

  const auto oriented = ExerciseOrientation::orient(relativePitch, relativeRoll, currentExercise.useRoll, currentExercise.direction);
  bool pitchWithinLimits =
    oriented.pitch >= currentExercise.limits.minRelativePitch &&
    oriented.pitch <= currentExercise.limits.maxRelativePitch;

  bool rollWithinLimits =
    oriented.roll >= currentExercise.limits.minRelativeRoll &&
    oriented.roll <= currentExercise.limits.maxRelativeRoll;

  bool jointWithinLimits =
    pitchWithinLimits && rollWithinLimits;

  String status = "WAITING";

  motionWarning = sessionActive && !jointWithinLimits;
  if (motionWarning) {
    status = generateLimitWarning(
      oriented.pitch,
      oriented.roll,
      pitchWithinLimits,
      rollWithinLimits
    );

    movementState = WAITING_FOR_START;
    startPositionSeen = false;

    setLedMode(LED_WARNING);

    sendStatus(
      proximalPitch,
      proximalRoll,
      distalPitch,
      distalRoll,
      relativePitch,
      relativeRoll,
      primaryAngle,
      movementSpeed,
      status
    );

    return;
  }

  if (ledMode == LED_WARNING) {
    setLedMode(
      bleClientConnected
        ? LED_CONNECTED
        : LED_ACTIVE
    );
  }

  bool raised =
    primaryAngle >= currentExercise.raisedThreshold;

  bool lowered =
    primaryAngle <= currentExercise.loweredThreshold;


  if (!sessionActive) {
    sendStatus(proximalPitch, proximalRoll, distalPitch, distalRoll, relativePitch,
               relativeRoll, primaryAngle, movementSpeed,
               targetRepetitions > 0 && repetitionCount >= targetRepetitions ? "SET COMPLETE" : "READY");
    return;
  }

  switch (movementState) {
    case WAITING_FOR_START:
      if (lowered) startPositionSeen = true;
      if (startPositionSeen && !lowered && now - lastRepTime >= currentExercise.cooldownMs) {
        movementState = RAISING;
        raiseStartTime = now;
        peakRaiseSpeed = abs(movementSpeed);
        status = "RAISING";
      } else if (lowered) {
        status = "INITIAL POSITION";
      } else {
        status = "MOVE TO START";
      }
      break;

    case RAISING:
      peakRaiseSpeed = max(peakRaiseSpeed, abs(movementSpeed));
      if (lowered) {
        movementState = WAITING_FOR_START;
        invalidRepetitionCount++;
        status = "INCOMPLETE MOVEMENT";
      } else if (
        raised && peakRaiseSpeed >= currentExercise.minimumMovementSpeed &&
        now - raiseStartTime >= currentExercise.minimumRaiseTimeMs
      ) {
        movementState = HOLDING;
        holdStartTime = now;
        status = "HOLD";
      } else {
        status = "RAISING";
      }
      break;

    case HOLDING:
      if (!raised) {
        movementState = WAITING_FOR_START;
        startPositionSeen = false;
        invalidRepetitionCount++;
        status = "HOLD LOST";
      } else if (
        now - holdStartTime >= currentExercise.holdTimeMs
      ) {
        movementState = RETURNING;
        status = "RETURN TO START";
      } else {
        float progress =
          100.0f *
          (now - holdStartTime) /
          currentExercise.holdTimeMs;

        status =
          "HOLDING " +
          String((int)constrain(progress, 0.0f, 100.0f)) +
          "%";
      }
      break;

    case RETURNING:
      if (lowered) {
        repetitionCount++;
        lastRepTime = now;
        movementState = WAITING_FOR_START;

        status =
          "REP COMPLETE " +
          String(repetitionCount);

        showTemporaryLed(LED_SUCCESS, 800);
        if (targetRepetitions > 0 && repetitionCount >= targetRepetitions) {
          sessionActive = false;
          status = "SET COMPLETE";
        }
      } else {
        status = "RETURNING";
      }
      break;
  }

  sendStatus(
    proximalPitch,
    proximalRoll,
    distalPitch,
    distalRoll,
    relativePitch,
    relativeRoll,
    primaryAngle,
    movementSpeed,
    status
  );
}

// ======================================================
// BUTTON FUNCTIONS
// ======================================================

void initializeButton() {
  pinMode(BUTTON_PIN, INPUT); // Driven by the TTP223 digital output.
}

bool isButtonPressed() {
  return digitalRead(BUTTON_PIN) == HIGH; // Active only while touched.
}

ButtonAction readButtonAction() {
  const bool pressed = isButtonPressed();
  const unsigned long now = millis();
  if (pressed != rawButtonPressed) {
    rawButtonPressed = pressed;
    lastButtonChangeAt = now;
  }
  if (now - lastButtonChangeAt < BUTTON_DEBOUNCE_MS) return BUTTON_NO_ACTION;
  // A button held during boot must first be released.
  if (!buttonArmed) {
    if (!pressed) buttonArmed = true;
    return BUTTON_NO_ACTION;
  }
  if (pressed == previousButtonPressed) return BUTTON_NO_ACTION;
  previousButtonPressed = pressed;
  if (pressed) { buttonPressedAt = now; return BUTTON_NO_ACTION; }
  const unsigned long duration = now - buttonPressedAt;
  if (duration >= LONG_PRESS_MS) return BUTTON_LONG_PRESS;
  if (duration >= MEDIUM_PRESS_MS) return BUTTON_MEDIUM_PRESS;
  return duration >= BUTTON_DEBOUNCE_MS ? BUTTON_SHORT_PRESS : BUTTON_NO_ACTION;
}



void handleButtonAction(ButtonAction action) {
  switch (action) {
    case BUTTON_SHORT_PRESS:
      calibrationActive = false;
      resetExerciseSession();

      Serial.println("Button: repetition counter reset.");
      notifyBleMessage("SESSION RESET");

      showTemporaryLed(LED_SUCCESS, 500);
      break;

    case BUTTON_MEDIUM_PRESS:
      Serial.println("Button: sensor recalibration requested.");

      calibrateBothSensors();
      break;

    case BUTTON_LONG_PRESS:
      Serial.println("Button: hardware restart requested.");

      calibrationActive = false;
      resetExerciseSession();
      sensorsCalibrated = false;
      notifyBleMessage("SESSION RESET");
      setLedMode(LED_ADVERTISING);
      delay(100); // Allow the reset notification to leave before rebooting.
      ESP.restart();
      break;

    case BUTTON_NO_ACTION:
      break;
  }
}

void resetExerciseSession() {
  sessionActive = false;
  startPositionSeen = false;
  peakRaiseSpeed = 0;
  previousSpeedTime = 0;
  repetitionCount = 0;
  invalidRepetitionCount = 0;

  movementState = WAITING_FOR_START;

  raiseStartTime = 0;
  holdStartTime = 0;
  lastRepTime = millis() - currentExercise.cooldownMs;
  targetRepetitions = 0;
  motionWarning = false;
}

// CALIBRATE2:name + 13 numeric fields; START2 starts exactly that calibrated prescription.
void handleCommand(
  const String &command,
  const String &payload
) {
  Serial.println(command);
  if (command == "STOP" || command == "RESET") {
    calibrationActive = false;
    resetExerciseSession();
    notifyBleMessage("SESSION RESET");
    return;
  }
  if (sessionActive) {
    notifyBleMessage("ERROR:SESSION ACTIVE");
    return;
  }

  // If this callback is being processed, the Android device is already able
  // to write to the GATT characteristic. Do not reject valid commands because
  // the local BLE2902 notification flag or getPeerMTU() has not yet reflected
  // the Android-side negotiation.
  if (!bleClientConnected || !bleServer || !bleCharacteristic) {
    notifyBleMessage("ERROR:NOT CONNECTED");
    return;
  }

  if (calibrationActive) {
    notifyBleMessage("ERROR:CALIBRATION ACTIVE");
    return;
  }

  // Recalibrate the profile that is already configured.
  if (command == "CALIBRATE") {
    if (!exerciseConfigured) {
      notifyBleMessage("ERROR:PROFILE REQUIRED");
      return;
    }

    calibrateBothSensors();
    return;
  }

  // CALIBRATE2 accepts both formats:
  //
  // 1) With exercise name:
  // name,reps,raise,lower,minRaiseMs,holdMs,cooldownMs,minSpeed,
  // minPitch,maxPitch,minRoll,maxRoll,useRoll,direction
  //
  // 2) Without exercise name:
  // reps,raise,lower,minRaiseMs,holdMs,cooldownMs,minSpeed,
  // minPitch,maxPitch,minRoll,maxRoll,useRoll,direction
  //
  // This keeps the firmware compatible with both Android command formats.
  if (command == "CALIBRATE2") {
    char exerciseName[64] = {};

    int reps = 0;
    int axis = 0;
    int direction = 1;
    int consumed = 0;

    unsigned long minRaise = 0;
    unsigned long hold = 0;
    unsigned long cooldown = 0;

    float raised = 0.0f;
    float lowered = 0.0f;
    float speed = 0.0f;
    float minPitch = 0.0f;
    float maxPitch = 0.0f;
    float minRoll = 0.0f;
    float maxRoll = 0.0f;

    int count = 0;
    bool hasExerciseName = false;

    // If the first token starts with a number/sign, the payload is the
    // 13-field format without an exercise name.
    const char firstChar = payload.length() > 0 ? payload.charAt(0) : '\0';
    const bool startsNumeric =
      (firstChar >= '0' && firstChar <= '9') ||
      firstChar == '-' ||
      firstChar == '+';

    if (startsNumeric) {
      count = sscanf(
        payload.c_str(),
        "%d,%f,%f,%lu,%lu,%lu,%f,%f,%f,%f,%f,%d,%d%n",
        &reps,
        &raised,
        &lowered,
        &minRaise,
        &hold,
        &cooldown,
        &speed,
        &minPitch,
        &maxPitch,
        &minRoll,
        &maxRoll,
        &axis,
        &direction,
        &consumed
      );

      strncpy(exerciseName, "App configured exercise", sizeof(exerciseName) - 1);
      exerciseName[sizeof(exerciseName) - 1] = '\0';
    } else {
      hasExerciseName = true;

      count = sscanf(
        payload.c_str(),
        "%63[^,],%d,%f,%f,%lu,%lu,%lu,%f,%f,%f,%f,%f,%d,%d%n",
        exerciseName,
        &reps,
        &raised,
        &lowered,
        &minRaise,
        &hold,
        &cooldown,
        &speed,
        &minPitch,
        &maxPitch,
        &minRoll,
        &maxRoll,
        &axis,
        &direction,
        &consumed
      );
    }

    const int expectedCount = hasExerciseName ? 14 : 13;

    Serial.print("Parsed fields: ");
    Serial.println(count);
    Serial.print("Exercise name: ");
    Serial.println(exerciseName);

    const bool invalidProfile =
      count != expectedCount ||
      consumed != (int)payload.length() ||
      reps < 1 || reps > 200 ||
      !isfinite(raised) || !isfinite(lowered) || !isfinite(speed) ||
      !isfinite(minPitch) || !isfinite(maxPitch) ||
      !isfinite(minRoll) || !isfinite(maxRoll) ||
      raised <= lowered ||
      speed < 0 ||
      minRaise > 60000 || hold > 60000 || cooldown > 60000 ||
      minPitch < -180 || maxPitch > 180 || minPitch >= maxPitch ||
      minRoll < -180 || maxRoll > 180 || minRoll >= maxRoll ||
      (axis != 0 && axis != 1) ||
      (direction != -1 && direction != 1) ||
      lowered < (axis ? minRoll : minPitch) ||
      raised > (axis ? maxRoll : maxPitch);

    if (invalidProfile) {
      Serial.println("Invalid CALIBRATE2 payload: " + payload);
      notifyBleMessage("ERROR:INVALID PROFILE");
      return;
    }

    resetExerciseSession();
    sensorsCalibrated = false;

    currentExercise.name = String(exerciseName);
    currentExercise.useRoll = axis == 1;
    currentExercise.direction = direction;
    currentExercise.raisedThreshold = raised;
    currentExercise.loweredThreshold = lowered;
    currentExercise.minimumRaiseTimeMs = minRaise;
    currentExercise.holdTimeMs = hold;
    currentExercise.cooldownMs = cooldown;
    currentExercise.minimumMovementSpeed = speed;
    currentExercise.limits = {
      minPitch,
      maxPitch,
      minRoll,
      maxRoll
    };

    // Preserve the requested repetitions across calibration. resetExerciseSession()
    // intentionally clears targetRepetitions, so START2 restores it from here.
    configuredRepetitions = reps;
    exerciseConfigured = true;

    Serial.println("Exercise configuration received from Android.");
    Serial.println("Reps: " + String(configuredRepetitions));
    Serial.println("Target: " + String(currentExercise.raisedThreshold));
    Serial.println("Return: " + String(currentExercise.loweredThreshold));
    Serial.println("Axis: " + String(currentExercise.useRoll ? "ROLL" : "PITCH"));
    Serial.println("Direction: " + String(currentExercise.direction));

    notifyBleMessage("PROFILE ACCEPTED");
    calibrateBothSensors();
    return;
  }

  if (command == "START2") {
    if (!exerciseConfigured || configuredRepetitions < 1) {
      notifyBleMessage("ERROR:PROFILE REQUIRED");
      return;
    }

    if (!sensorsCalibrated || sensorFault || !sensorsReady) {
      notifyBleMessage("ERROR:CALIBRATION REQUIRED");
      return;
    }

    resetExerciseSession();
    targetRepetitions = configuredRepetitions;
    lastRepTime = millis() - currentExercise.cooldownMs;
    resetFilters();

    // Start telemetry only from this point onward.
    lastSensorReadTime = millis();
    sessionActive = true;

    Serial.println("SESSION STARTED");
    setLedMode(LED_ACTIVE);
    notifyBleMessage("SESSION STARTED");
    return;
  }

  notifyBleMessage("ERROR:UNKNOWN COMMAND");
}

// ======================================================
// LED FUNCTIONS
// ======================================================

void initializeLed() {
  pinMode(RED_PIN, OUTPUT);
  pinMode(GREEN_PIN, OUTPUT);
  pinMode(BLUE_PIN, OUTPUT);

  analogWrite(RED_PIN, 0);
  analogWrite(GREEN_PIN, 0);
  analogWrite(BLUE_PIN, 0);
}

void setLedMode(LedMode mode) { ledMode = mode; }

void showTemporaryLed(LedMode mode, unsigned long durationMs) {
  ledMode = mode;
  temporaryLedUntil = millis() + durationMs;
}

void setLedColor(uint8_t red, uint8_t green, uint8_t blue) {
  analogWrite(RED_PIN, red);
  analogWrite(GREEN_PIN, green);
  analogWrite(BLUE_PIN, blue);
}

void updateLed() {
  const unsigned long now = millis();
  LedMode mode;
  if (sensorFault || motionWarning) mode = LED_WARNING;
  else if (calibrationActive) mode = LED_CALIBRATING;
  else if (buttonArmed && previousButtonPressed && now - buttonPressedAt >= LONG_PRESS_MS) mode = LED_ADVERTISING;
  else if (buttonArmed && previousButtonPressed && now - buttonPressedAt >= MEDIUM_PRESS_MS) mode = LED_CALIBRATING;
  else if (temporaryLedUntil != 0 && (int32_t)(now - temporaryLedUntil) < 0) mode = ledMode;
  else {
    temporaryLedUntil = 0;
    mode = sessionActive ? LED_ACTIVE : (bleClientConnected ? LED_CONNECTED : LED_ADVERTISING);
  }
  switch (mode) {
    case LED_BOOTING: setLedColor(30,30,30); break;
    case LED_ADVERTISING: setLedColor(0,0,(now / 350) % 2 ? 80 : 0); break;
    case LED_CONNECTED:
      if (sensorsCalibrated) setLedColor(0,40,60);
      else setLedColor(0,(now / 700) % 2 ? 40 : 0,(now / 700) % 2 ? 60 : 0);
      break;
    case LED_CALIBRATING: setLedColor((now / 150) % 2 ? 80 : 15,40,0); break;
    case LED_ACTIVE: setLedColor(30,0,50); break;
    case LED_SUCCESS: setLedColor(0,80,0); break;
    case LED_WARNING: setLedColor((now / 200) % 2 ? 100 : 0,0,0); break;
    default: setLedColor(0,0,0); break;
  }
}


// ======================================================
// BLE FUNCTIONS
// ======================================================

void initializeBle() {
  commandQueue = xQueueCreate(8, sizeof(QueuedCommand));
  if (!commandQueue) { Serial.println("Fatal: command queue allocation failed"); ESP.restart(); }
  BLEDevice::init("ESP32-C3_DUAL_IMU");

  /*
   * Dual-sensor messages exceed the default 20-byte
   * notification payload. Android should also request
   * an MTU of 247 bytes (up to 244 payload bytes).
   */
  BLEDevice::setMTU(247);

  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new BleServerCallbacks());

  BLEService *service =
    bleServer->createService(BLE_SERVICE_UUID);

  bleCharacteristic =
    service->createCharacteristic(
      BLE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
      BLECharacteristic::PROPERTY_WRITE |
      BLECharacteristic::PROPERTY_NOTIFY
    );

  notificationDescriptor = new BLE2902();
  bleCharacteristic->addDescriptor(notificationDescriptor);
  bleCharacteristic->setCallbacks(new SensorCommandCallbacks());
  bleCharacteristic->setValue("READY");

  service->start();

  BLEAdvertising *advertising =
    BLEDevice::getAdvertising();

  advertising->addServiceUUID(BLE_SERVICE_UUID);
  advertising->setScanResponse(true);

  BLEDevice::startAdvertising();

  setLedMode(LED_ADVERTISING);

  Serial.println("BLE advertising as ESP32-C3_DUAL_IMU.");
}

void notifyBleMessage(const String &message) {
  if (!bleClientConnected || bleCharacteristic == nullptr || bleServer == nullptr) {
    Serial.println("BLE control message not sent (not connected): " + message);
    return;
  }

  Serial.println("BLE TX: " + message);
  bleCharacteristic->setValue(message.c_str());

  // Do not depend on BLE2902::getNotifications() here. On some ESP32 BLE
  // stacks the local descriptor state can lag behind the CCCD write performed
  // by Android. notify() is safe to call after connection; only subscribed
  // clients will receive the notification.
  const uint16_t peerMtu = bleServer->getPeerMTU(bleServer->getConnId());
  const size_t maxPayload = peerMtu > 3 ? (size_t)(peerMtu - 3) : 20;

  if (message.length() <= maxPayload) {
    bleCharacteristic->notify();
  } else {
    Serial.println(
      "BLE notification skipped: message exceeds negotiated MTU (" +
      String(peerMtu) + ")."
    );
  }
}

void notifyBleSensorData(
  float relativePitch,
  float relativeRoll,
  float primaryAngle,
  int repetitions,
  float speed,
  const String &status
) {
  if (!bleClientConnected || bleCharacteristic == nullptr) {
    return;
  }

  String safeStatus = status;
  safeStatus.replace('|', '/');

  /*
   * Compact BLE payload:
   * P = relative pitch
   * R = relative roll
   * A = selected joint angle
   * N = repetition count
   * V = angular velocity
   * S = state
   */
  String payload =
    "P:" + String(relativePitch, 1) +
    "|R:" + String(relativeRoll, 1) +
    "|A:" + String(primaryAngle, 1) +
    "|N:" + String(repetitions) +
    "|V:" + String(speed, 1) +
    "|S:" + safeStatus;

  notifyBleMessage(payload);
}

// ======================================================
// OUTPUT
// ======================================================

void sendStatus(
  float proximalPitch,
  float proximalRoll,
  float distalPitch,
  float distalRoll,
  float relativePitch,
  float relativeRoll,
  float primaryAngle,
  float speed,
  const String &status
) {

  Serial.print("PROX[P:");
  Serial.print(proximalPitch, 1);
  Serial.print(" R:");
  Serial.print(proximalRoll, 1);

  Serial.print("] DIST[P:");
  Serial.print(distalPitch, 1);
  Serial.print(" R:");
  Serial.print(distalRoll, 1);

  Serial.print("] REL[P:");
  Serial.print(relativePitch, 1);
  Serial.print(" R:");
  Serial.print(relativeRoll, 1);

  Serial.print("] ANG:");
  Serial.print(primaryAngle, 1);

  Serial.print(" REP:");
  Serial.print(repetitionCount);

  Serial.print(" VEL:");
  Serial.print(speed, 1);

  Serial.print(" STATUS:");
  Serial.println(status);

  notifyBleSensorData(
    relativePitch,
    relativeRoll,
    primaryAngle,
    repetitionCount,
    speed,
    status
  );
}

// ======================================================
// FILTER AND ANGLE HELPERS
// ======================================================

float applyMovingAverage(
  MovingAverageFilter &filter,
  float newValue
) {
  if (!filter.initialized) {
    for (int i = 0; i < FILTER_SIZE; i++) {
      filter.values[i] = newValue;
    }

    filter.initialized = true;
    filter.index = 0;
  }

  filter.values[filter.index] = newValue;
  filter.index = (filter.index + 1) % FILTER_SIZE;

  float sinSum = 0.0f, cosSum = 0.0f;

  for (int i = 0; i < FILTER_SIZE; i++) {
    sinSum += sinf(filter.values[i] * DEG_TO_RAD);
    cosSum += cosf(filter.values[i] * DEG_TO_RAD);
  }

  return atan2f(sinSum, cosSum) * RAD_TO_DEG;
}

void resetFilters() {
  relativePitchFilter.initialized = false;
  relativePitchFilter.index = 0;

  relativeRollFilter.initialized = false;
  relativeRollFilter.index = 0;
}

float normalizeAngle(float angle) {
  if (!isfinite(angle)) return NAN;
  while (angle > 180.0f) {
    angle -= 360.0f;
  }

  while (angle < -180.0f) {
    angle += 360.0f;
  }

  return angle;
}

float calculateMovementSpeed(float currentAngle) {

  unsigned long now = millis();

  if (previousSpeedTime == 0) {
    previousSpeedAngle = currentAngle;
    previousSpeedTime = now;
    return 0.0f;
  }

  float elapsedSeconds =
    (now - previousSpeedTime) / 1000.0f;

  if (elapsedSeconds <= 0.0f) {
    return 0.0f;
  }

  float angleDifference =
    normalizeAngle(currentAngle - previousSpeedAngle);

  float speed =
    angleDifference / elapsedSeconds;

  previousSpeedAngle = currentAngle;
  previousSpeedTime = now;

  return speed;
}

String generateLimitWarning(
  float relativePitch,
  float relativeRoll,
  bool pitchWithinLimits,
  bool rollWithinLimits
) {
  String warning = "LIMIT";

  if (!pitchWithinLimits) {
    warning += relativePitch >
      currentExercise.limits.maxRelativePitch
        ? " PITCH HIGH"
        : " PITCH LOW";
  }

  if (!rollWithinLimits) {
    warning += relativeRoll >
      currentExercise.limits.maxRelativeRoll
        ? " ROLL HIGH"
        : " ROLL LOW";
  }

  return warning;
}

// ======================================================
// I2C DIAGNOSTICS
// ======================================================

void scanI2CBus() {
  Serial.println("Scanning I2C bus...");

  int deviceCount = 0;

  for (uint8_t address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    uint8_t error = Wire.endTransmission();

    if (error == 0) {
      Serial.print("I2C device found at 0x");

      if (address < 0x10) {
        Serial.print("0");
      }

      Serial.println(address, HEX);
      deviceCount++;
    }
  }

  if (deviceCount == 0) {
    Serial.println("No I2C devices detected.");
  }

  Serial.println();
}

void printButtonInstructions() {
  Serial.println();
  Serial.println("Button functions:");
 Serial.println("- Short press: reset repetitions");
  Serial.println("- Medium press (1.5 s): recalibrate both IMUs");
  Serial.println("- Long press (4 s), then release: restart hardware and reconnect in the app");

  Serial.println();
}
