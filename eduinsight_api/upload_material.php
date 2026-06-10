<?php
header('Content-Type: application/json');
error_reporting(0); // Prevents text errors from breaking the app

$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database connection failed"]));
}

$inst_id = $_POST['institution_id'] ?? '';
$uploader_id = $_POST['uploader_id'] ?? '';
$type = $_POST['type'] ?? '';
$dept_id = $_POST['department_id'] ?? '';
$subject = $_POST['subject_name'] ?? '';
$file_name = $_POST['file_name'] ?? '';
$file_data = $_POST['file_data'] ?? '';

if (empty($file_data)) {
    die(json_encode(["status" => "error", "message" => "No file data received. Check post_max_size in php.ini"]));
}

$upload_dir = "uploads/";
if (!is_dir($upload_dir)) { mkdir($upload_dir, 0777, true); }

$decoded_file = base64_decode($file_data);
$safe_name = time() . "_" . preg_replace("/[^a-zA-Z0-9._]/", "_", $file_name);
$file_path = $upload_dir . $safe_name;

if (file_put_contents($file_path, $decoded_file)) {
    // We check if the statement prepares successfully
    $sql = "INSERT INTO materials (institution_id, uploader_id, type, department_id, subject_name, file_name, file_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($sql);
    
    if (!$stmt) {
        die(json_encode(["status" => "error", "message" => "SQL Prepare Failed: " . $conn->error]));
    }

    $stmt->bind_param("iisssss", $inst_id, $uploader_id, $type, $dept_id, $subject, $file_name, $file_path);
    
    if ($stmt->execute()) {
        /**
         * NOTIFICATION LOGIC: Notify students in the same institution and department
         */
        try {
            if (file_exists('fcm_helper.php')) {
                include_once 'fcm_helper.php';

                // Get uploader name
                $uploader_name = "Teacher";
                $u_res = $conn->query("SELECT name FROM users WHERE id = $uploader_id LIMIT 1");
                if ($u_row = $u_res->fetch_assoc()) {
                    $uploader_name = $u_row['name'];
                }

                $notif_title = "New Material Available 📚";
                $notif_body = "$uploader_name uploaded $file_name for $subject";

                // Target: All students in the same institution and department
                $query_students = "SELECT fcm_token FROM users WHERE institution_id = ? AND department_id = ? AND role = 'student' AND fcm_token IS NOT NULL";
                $st_stmt = $conn->prepare($query_students);
                if ($st_stmt) {
                    $st_stmt->bind_param("ii", $inst_id, $dept_id);
                    $st_stmt->execute();
                    $res = $st_stmt->get_result();
                    while ($row = $res->fetch_assoc()) {
                        $token = $row['fcm_token'];
                        if (!empty($token)) {
                            sendFCM($token, $notif_title, $notif_body, "material", $type, $uploader_id, "teacher");
                        }
                    }
                    $st_stmt->close();
                }
            }
        } catch (Exception $e) { }

        echo json_encode(["status" => "success", "message" => "Uploaded successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Execution Error: " . $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Failed to save file to server folder"]);
}

$conn->close();
?>