BEGIN;

INSERT INTO dbo.exercise_info (
    id, title, description, exercise_type, supports_camera, supports_sensors, use_roll, movement_direction, raise_threshold, lower_threshold, min_raise_time_ms, hold_time_ms, cooldown_ms, min_movement_speed, min_pitch, max_pitch, min_roll, max_roll, camera_joint, camera_movement
) VALUES
    ('73a20000-cc31-4c29-9000-000000000001', 'Wrist Flexion', 'Support your forearm with your palm facing down and your hand free beyond the edge of the support. Start with your wrist aligned with your forearm. Slowly bend your hand downward through the range prescribed by your physiotherapist. Hold for the prescribed time, then return to the starting position. Keep your forearm still and perform the prescribed repetitions for each indicated side.', 'Forearms', TRUE, TRUE, FALSE, 1, 30, 5, 1000, 5000, 1000, 0, -10, 60, -20, 20, 'WRIST', 'FLEXION'),

    ('73a20000-cc31-4c29-9000-000000000002', 'Wrist Extension', 'Support your forearm with your palm facing down and your hand free beyond the edge of the support. Start with your wrist aligned with your forearm. Slowly lift your hand upward through the range prescribed by your physiotherapist. Hold for the prescribed time, then return to the starting position. Keep your forearm still and perform the prescribed repetitions for each indicated side.', 'Forearms', TRUE, TRUE, FALSE, -1, 30, 5, 1000, 5000, 1000, 0, -10, 60, -30, 30, 'WRIST', 'EXTENSION'),

    ('73a20000-cc31-4c29-9000-000000000003', 'Elbow Flexion', 'Sit or stand with your upper arm supported close to your body and your arm comfortably lowered. Slowly bend your elbow through the range prescribed by your physiotherapist, without lifting your upper arm or rotating your forearm. Hold for the prescribed time, then slowly return to the starting position. Perform the prescribed repetitions for each indicated side.', 'Biceps', TRUE, TRUE, FALSE, -1, 60, 15, 1000, 2000, 1000, 0, -10, 130, -25, 25, 'ELBOW', 'FLEXION'),

    ('73a20000-cc31-4c29-9000-000000000004', 'Elbow Extension', 'Support your upper arm and start with your elbow comfortably bent, as instructed by your physiotherapist. Slowly straighten your elbow through the prescribed range without forcing or locking it. Keep your upper arm still and avoid rotating your forearm. Hold for the prescribed time, then return to the same bent starting position. Perform the prescribed repetitions for each indicated side.', 'Triceps', TRUE, TRUE, FALSE, 1, 60, 10, 1000, 2000, 1000, 0, -10, 100, -25, 25, 'ELBOW', 'EXTENSION'),

    ('73a20000-cc31-4c29-9000-000000000005', 'Knee Flexion - Heel Slide', 'Lie on your back with your leg comfortably extended and your heel resting on the surface. Slowly slide your heel toward your buttock to bend your knee through the range prescribed by your physiotherapist. Hold for the prescribed time, then slide your heel away to return to the starting position. Keep your leg aligned and avoid twisting. Perform the prescribed repetitions for each indicated side.', 'Legs', TRUE, TRUE, TRUE, 1, 60, 15, 1000, 5000, 1000, 0, -25, 25, -10, 120, 'KNEE', 'FLEXION'),

    ('73a20000-cc31-4c29-9000-000000000006', 'Knee Extension - Seated', 'Sit on a stable chair with your thigh supported and your knee comfortably bent. Slowly lift your foot by straightening your knee through the range prescribed by your physiotherapist. Keep your thigh still and avoid forcing or locking your knee. Hold for the prescribed time, then slowly return to the starting position. Perform the prescribed repetitions for each indicated side.', 'Legs', TRUE, TRUE, FALSE, -1, 60, 10, 1000, 5000, 1000, 0, -10, 100, -25, 55, 'KNEE', 'EXTENSION'),

    ('73a20000-cc31-4c29-9000-000000000007', 'Knee Flexion - Standing', 'Stand upright and hold a stable support for balance. Start with the leg you will exercise comfortably straight. Keeping your thighs aligned, slowly bring your heel backward to bend your knee through the range prescribed by your physiotherapist. Hold for the prescribed time, then lower your foot to the starting position. Avoid leaning or moving your thigh forward. Perform the prescribed repetitions for each indicated side.', 'Legs', TRUE, TRUE, TRUE, 1, 60, 15, 1000, 5000, 1000, 0, -25, 25, -10, 120, 'KNEE', 'FLEXION'),

    ('73a20000-cc31-4c29-9000-000000000008', 'Supported Mini Squat', 'Stand with both feet firmly on the floor and hold a stable support for balance. Slowly bend your knees and hips to the depth prescribed by your physiotherapist, keeping your heels down and your knees aligned with your feet. Hold for the prescribed time, then return to standing in a controlled movement. Perform the prescribed repetitions.', 'Legs', TRUE, FALSE, TRUE, 1, 30, 8, 1000, 2000, 1000, 0, -25, 25, -10, 60, 'KNEE', 'FLEXION')

ON CONFLICT (id) DO UPDATE
SET description = EXCLUDED.description;

COMMIT;
