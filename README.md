<h1 align="center">
  <img src="docs/imgs/logo.png" alt="IPC logo" width="120"><br>
  IPCare
</h1>

> Developed in the scope of a master's thesis in Computer Science, at NOVA FCT.

IPCare is a rehabilitation monitoring research project that connects an Android application, a Kotlin backend and a wearable prototype. This version extends the original Intelligent Personalized Care bachelor project in the context of a master’s dissertation in Computer Science and Engineering.

Patients follow prescribed exercise plans using either camera-based movement tracking or two wearable inertial sensors. Physiotherapists manage plans, configure exercise targets and review recorded sessions and patient progress. The catalogue focuses on the **wrist, elbow and knee**.

## What the system does

- **Patients:** consult assigned plans, filter the exercise library by joint, follow demonstrations, perform exercises and report whether external load was used, including its weight.
- **Physiotherapists:** maintain a plan library, assign plans to patients, customise exercise parameters and provide written feedback and a score from one to five stars.
- **Camera mode:** CameraX captures video while Google ML Kit supplies pose landmarks. The application calculates joint angles, calibrates the starting position and counts repetitions using the prescribed target, hold and return conditions.
- **Sensor mode:** the application obtains the effective exercise profile from the backend and sends it over BLE to an ESP32-C3. Two ICM-20948 sensors measure relative segment movement. Calibration and execution status are shown in the app and indicated by the wearable LED.
- **Session history:** repeated plan assignments retain separate histories. Camera recordings and sensor sessions can be reviewed by the associated physiotherapist.
- **Usability:** English and Portuguese UI resources, spoken exercise feedback and a guided sensor preparation flow.
- **Video recovery:** completed recordings are copied to the phone Gallery. Failed uploads remain pending, and patients can save a set locally and continue before retrying submission.

This is a research prototype. Automated checks verify software behaviour, but do not establish clinical accuracy, clinical effectiveness or medical-device certification.

## Architecture

The Android application communicates with a REST API using access and refresh tokens. The backend uses controllers, services and JDBI repositories, with PostgreSQL for structured data and Google Cloud Storage for media. Server-sent events deliver application updates.

Camera processing runs on the phone. The wearable communicates with the phone through BLE and does not connect directly to the backend. Exercise settings are resolved from catalogue defaults and applicable plan/assignment overrides rather than hardcoded exercise definitions in the firmware.

The wearable contains two physical modules: a main module with the ESP32-C3, one ICM-20948, a TTP223 touch sensor and an RGB LED, and a second module containing the additional ICM-20948. Both sensors are wired to the same controller. The main module is powered by a power bank incorporated into its Velcro mounting.

## Test the App

Scan this Qr Code to download the app:
<h1 align="left">
    <img src="docs/imgs/qrcode.png" alt="App QR Code" width="150">
</h1>

## Repository contents

- `frontend/` - Android application, JVM tests and Android instrumentation tests.
- `backend/` - Spring Boot API, PostgreSQL schema/catalogue and backend tests.
- `arduino/` - wearable firmware. Check the variant and its pin definitions against the physical board before flashing.
- `docs/` - API specification, Postman collection, integration notes and project material.


## Project origins and acknowledgements

The original IPC bachelor project was developed at [ISEL](https://www.isel.pt/) by [Guilherme Cepeda](https://github.com/bodeborder), [Rodrigo Neves](https://github.com/RodrigoNevesWork) and [Tiago Martinho](https://github.com/tiagomartinhoo), supervised by [Paulo Pereira](https://github.com/palbp). This repository contains its subsequent rehabilitation-monitoring extensions.

## Author

* [Tiago Martinho](https://github.com/tiagomartinhoo)

## Supervisors

* Carmen Morgado, NOVA FCT
* Fernanda Barbosa, NOVA FCT