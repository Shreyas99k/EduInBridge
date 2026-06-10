<?php
error_reporting(E_ALL);
ini_set('display_errors', 1); // Temporarily enable to catch fatal errors in response
header('Content-Type: text/plain; charset=utf-8');

try {
    include 'db.php';

    // 1. Capture POST data
    $doubt_id = $_POST['doubt_id'] ?? null;
    $sender_id = $_POST['sender_id'] ?? null;
    $sender_type = strtolower(trim($_POST['sender_type'] ?? ''));
    $sender_name = $_POST['sender_name'] ?? null;
    $message = $_POST['message'] ?? '';
    $image = $_POST['image'] ?? null;
    $audio = $_POST['audio'] ?? null;
    $reply_msg = $_POST['reply_to_message'] ?? null;
    $reply_user = $_POST['reply_to_user'] ?? null;

    if ($sender_type === 'mentor') { $sender_type = 'teacher'; }

    if (!$doubt_id || !$sender_id) {
        die("missing_parameters");
    }

    // 2. Handle Multimedia uploads
    $image_path = null;
    if ($image) {
        if (!is_dir('uploads')) { mkdir('uploads', 0777, true); }
        $image_path = "uploads/" . uniqid() . ".jpg";
        file_put_contents($image_path, base64_decode($image));
    }

    $audio_path = null;
    if ($audio) {
        if (!is_dir('uploads')) { mkdir('uploads', 0777, true); }
        $audio_path = "uploads/" . uniqid() . ".m4a";
        file_put_contents($audio_path, base64_decode($audio));
    }

    // 3. INSERT into mentorship_chats
    $sql = "INSERT INTO mentorship_chats (doubt_id, sender_id, sender_role, message, image_url, audio_url, reply_to_message, reply_to_user) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        die("prepare_failed: " . $conn->error);
    }

    $stmt->bind_param("iissssss", $doubt_id, $sender_id, $sender_type, $message, $image_path, $audio_path, $reply_msg, $reply_user);

    if ($stmt->execute()) {
        $conn->query("UPDATE doubts SET has_new_message = 1 WHERE id = '$doubt_id'");

        // 4. Notification Logic (Wrapped in try-catch to avoid crashing if FCM fails)
        try {
            $res_rec = $conn->query("SELECT student_id, teacher_name FROM doubts WHERE id = '$doubt_id' LIMIT 1");
            if ($res_rec && $d_row = $res_rec->fetch_assoc()) {
                $target_token = "";
                if ($sender_type === 'student') {
                    $t_name = $d_row['teacher_name'];
                    $res_t = $conn->query("SELECT fcm_token FROM users WHERE name = '$t_name' AND role = 'teacher' LIMIT 1");
                    if ($t_row = $res_t->fetch_assoc()) { $target_token = $t_row['fcm_token']; }
                } else {
                    $s_id = $d_row['student_id'];
                    $res_s = $conn->query("SELECT fcm_token FROM users WHERE id = '$s_id' LIMIT 1");
                    if ($s_row = $res_s->fetch_assoc()) { $target_token = $s_row['fcm_token']; }
                }

                if (!empty($target_token) && file_exists('fcm_helper.php')) {
                    include_once 'fcm_helper.php';
                    $body = !empty($message) ? $message : ($image ? "Sent an image" : "Sent an audio");
                    // We check if sendFCM exists before calling to prevent fatal error
                    if (function_exists('sendFCM')) {
                        sendFCM($target_token, "New Message", $body, "chat", $doubt_id, $sender_id, $sender_type);
                    }
                }
            }
        } catch (Throwable $e) {
            // Log notification error but don't fail the message send
            file_put_contents('error_log.txt', "Notification failed: " . $e->getMessage() . "\n", FILE_APPEND);
        }

        echo "success";
    } else {
        echo "execute_failed: " . $stmt->error;
    }
    $stmt->close();
    $conn->close();

} catch (Throwable $t) {
    echo "Fatal Error: " . $t->getMessage() . " in " . $t->getFile() . " on line " . $t->getLine();
}
?>