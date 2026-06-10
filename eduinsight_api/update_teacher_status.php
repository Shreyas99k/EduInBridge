<?php
header('Content-Type: application/json');

// 1. Database Connection
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Get Data from Android App
$teacher_id = $_POST['teacher_id'] ?? 0;
$new_status = $_POST['status'] ?? ""; // Should be 'active' or 'rejected'

if (empty($teacher_id) || empty($new_status)) {
    echo json_encode(["status" => "error", "message" => "Missing required parameters"]);
    exit();
}

// 3. Validate status to prevent invalid data
if ($new_status !== 'active' && $new_status !== 'rejected') {
    echo json_encode(["status" => "error", "message" => "Invalid status value"]);
    exit();
}

// 4. Update Teacher's Status (Using Prepared Statement)
$stmt = $conn->prepare("UPDATE users SET status = ? WHERE id = ?");
$stmt->bind_param("si", $new_status, $teacher_id);

if ($stmt->execute()) {
    if ($stmt->affected_rows > 0) {
        /**
         * NOTIFICATION LOGIC: Notify the teacher about their account status
         */
        try {
            $t_res = $conn->query("SELECT fcm_token FROM users WHERE id = $teacher_id LIMIT 1");
            if ($t_row = $t_res->fetch_assoc()) {
                $token = $t_row['fcm_token'];
                if (!empty($token) && file_exists('fcm_helper.php')) {
                    include_once 'fcm_helper.php';
                    $title = ($new_status === 'active') ? "Account Approved! 🎉" : "Account Status Update";
                    $body = ($new_status === 'active')
                        ? "Congratulations! Your teacher account has been approved. You can now log in."
                        : "Your account request has been reviewed. Please contact the administrator.";

                    sendFCM($token, $title, $body, "account_status", "0", "0", "admin");
                }
            }
        } catch (Exception $e) { }

        echo json_encode(["status" => "success", "message" => "Teacher status updated successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Teacher not found or status is already updated"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Database update failed: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>