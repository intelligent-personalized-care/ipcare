#pragma once
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <cfloat>
#include <initializer_list>

namespace WearableProtocol {
constexpr size_t MAX_COMMAND_BYTES = 244;
constexpr size_t MAX_NAME_BYTES = 48;
struct Configuration { char name[MAX_NAME_BYTES + 1]{}; double values[13]{}; };
inline bool parseCalibration(const char *text, Configuration &result) {
  if (!text || std::strlen(text) > MAX_COMMAND_BYTES || std::strncmp(text, "CALIBRATE2:", 11) != 0) return false;
  const char *name = text + 11;
  const char *comma = std::strchr(name, ',');
  if (!comma || comma == name || size_t(comma - name) > MAX_NAME_BYTES) return false;
  for (const char *p = name; p < comma; ++p) if ((unsigned char)*p < 32 || *p == 127) return false;
  Configuration parsed{};
  std::memcpy(parsed.name, name, comma - name);
  const char *cursor = comma + 1;
  for (int i = 0; i < 13; ++i) {
    char *end = nullptr;
    errno = 0;
    parsed.values[i] = std::strtod(cursor, &end);
    if (end == cursor || errno == ERANGE || !std::isfinite(parsed.values[i]) ||
        (i < 12 ? *end != ',' : *end != '\0')) return false;
    cursor = end + (i < 12 ? 1 : 0);
  }
  const auto &v = parsed.values;
  for (int i : {0, 3, 4, 5, 11})
    if (v[i] < 0 || v[i] > 60000 || std::floor(v[i]) != v[i]) return false;
  if (v[0] < 1 || v[0] > 200 || (v[11] != 0 && v[11] != 1) || (v[12] != -1 && v[12] != 1) ||
      v[6] < 0 || !std::isfinite(static_cast<float>(v[6])) || v[1] <= v[2] ||
      v[7] < -180 || v[8] > 180 || v[7] >= v[8] ||
      v[9] < -180 || v[10] > 180 || v[9] >= v[10] ||
      v[2] < (v[11] == 1 ? v[9] : v[7]) || v[1] > (v[11] == 1 ? v[10] : v[8])) return false;
  result = parsed; // Commit only after complete validation.
  return true;
}
}
