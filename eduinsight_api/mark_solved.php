<?php
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 0);

try {
    include 'db.php';

    $doubt_id = $_POST['doubt_id'] ?? '';
    $solution = $_POST['solution'] ?? "Resolved via Discussion";

    if (empty($doubt_id)) {
        echo json_encode(["status" => "error", "message" => "Missing doubt ID"]);
        exit();
    }

    $check_col = $conn->query("SHOW COLUMNS FROM doubts LIKE 'has_new_message'");
    if ($check_col && $check_col->num_rows > 0) {
        $sql = "UPDATE doubts SET status = 'solved', solution_text = ?, has_new_message = 0 WHERE id = ?";
    } else {
        $sql = "UPDATE doubts SET status = 'solved', solution_text = ? WHERE id = ?";
    }

    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
        exit();
    }

    $stmt->bind_param("si", $solution, $doubt_id);
    if ($stmt->execute()) {
        // Background notification
        try {
            $token_sql = "SELECT u.fcm_token FROM users u JOIN doubts d ON u.id = d.student_id WHERE d.id = ? LIMIT 1";
            $t_stmt = $conn->prepare($token_sql);
            if ($t_stmt) {
                $t_stmt->bind_param("i", $doubt_id);
                $t_stmt->execute();
                $token_res = $t_stmt->get_result();
                if ($row = $token_res->fetch_assoc()) {
                    $token = $row['fcm_token'];
                    if (!empty($token) && file_exists('fcm_helper.php')) {
                        include_once 'fcm_helper.php';
                        if (function_exists('sendFCM')) {
                            sendFCM($token, "Doubt Resolved! ✅", "Your mentor has marked your doubt as solved.", "solution", $doubt_id);
                        }
                    }
                }
            }
        } catch (Throwable $e) {}

        echo json_encode(["status" => "success"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Execute failed: " . $stmt->error]);
    }
} catch (Throwable $t) {
    echo json_encode(["status" => "error", "message" => $t->getMessage()]);
}
?>