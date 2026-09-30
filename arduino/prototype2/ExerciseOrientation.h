#pragma once
namespace ExerciseOrientation {
// Primary-axis limits and targets share movement coordinates; cross-axis limits
// remain in calibrated sensor coordinates. P/R telemetry stays raw and signed.
struct Angles { float pitch; float roll; float primary; };
inline Angles orient(float pitch, float roll, bool useRoll, int direction) {
  return {useRoll ? pitch : pitch * direction,
          useRoll ? roll * direction : roll,
          (useRoll ? roll : pitch) * direction};
}
}
