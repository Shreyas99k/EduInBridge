<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: text/plain; charset=utf-8');

try {
    // 1. Database Connection
    include 'db.php';

    /**
     * 2. Capture POST data
     */
    $student_id   = $_POST['student_id'] ?? null;
    $student_name = $_POST['student_name'] ?? 'Unknown Student';
    $description  = $_POST['description'] ?? '';
    $subject      = $_POST['subject'] ?? '';
    $teacher_name = trim($_POST['teacher_name'] ?? '');
    $department   = $_POST['department'] ?? 'General';
    $sender_id    = $_POST['sender_id'] ?? '-1';

    if (!$student_id || empty($description) || empty($teacher_name)) {
        die("error: missing_parameters");
    }

    /**
     * 3. Insert the doubt
     */
    $sql = "INSERT INTO doubts (student_id, student_name, description, subject, department, teacher_name, status)
            VALUES (?, ?, ?, ?, ?, ?, 'pending')";

    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        die("error: SQL Prepare Failed: " . $conn->error);
    }

    $stmt->bind_param("isssss",
        $student_id,
        $student_name,
        $description,
        $subject,
        $department,
        $teacher_name
    );

    if ($stmt->execute()) {
        $stmt->close();

        /**
         * 4. NOTIFICATION LOGIC
         */
        try {
            $token_sql = "SELECT fcm_token FROM users WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND role = 'teacher' LIMIT 1";
            $t_stmt = $conn->prepare($token_sql);
            if ($t_stmt) {
                $t_stmt->bind_param("s", $teacher_name);
                $t_stmt->execute();
                $res = $t_stmt->get_result();
                if ($row = $res->fetch_assoc()) {
                    $token = $row['fcm_token'];
                    if (!empty($token) && file_exists('fcm_helper.php')) {
                        include_once 'fcm_helper.php';
                        if (function_exists('sendFCM')) {
                            sendFCM($token, "New Doubt", "$student_name asked in $subject", "doubt_posted", "0", $sender_id, "student");
                        }
                    }
                }
                $t_stmt->close();
            }
        } catch (Throwable $e) {
            // Log error but don't fail the request
            file_put_contents('error_log.txt', "Post Doubt Notification failed: " . $e->getMessage() . "\n", FILE_APPEND);
        }

        /**
         * 5. STREAK LOGIC
         */
        try {
            $today = date('Y-m-d');
            $streak_sql = "UPDATE users SET
                            streak = CASE
                                WHEN last_activity_date = ? THEN streak
                                WHEN last_activity_date >= DATE_SUB(?, INTERVAL 2 DAY) THEN streak + 1
                                ELSE 1
                            END,
                            last_activity_date = ?
                           WHERE id = ?";
            $s_stmt = $conn->prepare($streak_sql);
            if ($s_stmt) {
                $s_stmt->bind_param("sssi", $today, $today, $today, $student_id);
                $s_stmt->execute();
                $s_stmt->close();
            }
        } catch (Throwable $e) { }

        echo "success";
    } else {
        echo "error: " . $stmt->error;
    }

    $conn->close();

} catch (Throwable $t) {
    http_response_code(500);
    echo "Fatal Error: " . $t->getMessage() . " in " . $t->getFile() . " on line " . $t->getLine();
}
?>