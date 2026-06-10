<?php
include 'db.php';
header('Content-Type: application/json');

/**
 * 1. Capture POST data from the Android App
 */
$doubt_id = $_POST['doubt_id'] ?? '';
$solution = $_POST['solution'] ?? '';
$teacher_name = $_POST['teacher_name'] ?? 'Mentor';
$image_base64 = $_POST['image'] ?? '';
$sender_id = $_POST['sender_id'] ?? '-1'; 

if (empty($doubt_id)) {
    die(json_encode(["status" => "error", "message" => "Doubt ID missing"]));
}

/**
 * 2. Handle Multimedia Solution (Image & Audio)
 */
$image_url = "";
if (!empty($image_base64)) {
    if (!is_dir('uploads')) {
        mkdir('uploads', 0777, true);
    }
    $filename = "sol_" . $doubt_id . "_" . time() . ".jpg";
    $path = "uploads/" . $filename;

    if (file_put_contents($path, base64_decode($image_base64))) {
        $image_url = $path; 
    }
}

$audio_url = "";
$audio_base64 = $_POST['audio'] ?? '';
if (!empty($audio_base64)) {
    if (!is_dir('uploads')) {
        mkdir('uploads', 0777, true);
    }
    $filename = "sol_audio_" . $doubt_id . "_" . time() . ".m4a";
    $path = "uploads/" . $filename;

    if (file_put_contents($path, base64_decode($audio_base64))) {
        $audio_url = $path;
    }
}

/**
 * 3. Update Database: mark as 'solved'
 */
$sql = "UPDATE doubts SET solution_text = ?, teacher_name = ?, status = 'solved', sol_image = ?, sol_audio = ? WHERE id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ssssi", $solution, $teacher_name, $image_url, $audio_url, $doubt_id);

if ($stmt->execute()) {

    /**
     * 4. INSERT into 'mentorship_chats' so student sees the solution in chat
     */
    $chat_sql = "INSERT INTO mentorship_chats (doubt_id, sender_id, sender_role, message, image_url, audio_url) VALUES (?, ?, 'teacher', ?, ?, ?)";
    $c_stmt = $conn->prepare($chat_sql);
    if ($c_stmt) {
        $c_stmt->bind_param("iisss", $doubt_id, $sender_id, $solution, $image_url, $audio_url);
        $c_stmt->execute();
        $c_stmt->close();
    }

    /**
     * 5. NOTIFICATION LOGIC: Alert the student
     */
    try {
        $query_student = "SELECT student_id FROM doubts WHERE id = ?";
        $st_stmt = $conn->prepare($query_student);
        $st_stmt->bind_param("i", $doubt_id);
        $st_stmt->execute();
        $res = $st_stmt->get_result();

        if ($row = $res->fetch_assoc()) {
            $s_id = $row['student_id'];
            if ($s_id > 0) {
                $stmt_token = $conn->prepare("SELECT fcm_token FROM users WHERE id = ? LIMIT 1");
                $stmt_token->bind_param("i", $s_id);
                $stmt_token->execute();
                $res_token = $stmt_token->get_result();

                if ($u = $res_token->fetch_assoc()) {
                    $token = $u['fcm_token'];

                    if (!empty($token) && file_exists('fcm_helper.php')) {
                        include_once 'fcm_helper.php';
                        $notif_title = "Doubt Solved! ✅";
                        $notif_body = "Mentor $teacher_name has provided a solution to your doubt.";
                        $fcm_res = sendFCM($token, $notif_title, $notif_body, "solution", $doubt_id, $sender_id, "teacher");

                        // Debug log
                        file_put_contents('fcm_debug.txt', "Doubt $doubt_id | Student $s_id | Token: " . substr($token, 0, 10) . "... | Result: $fcm_res\n", FILE_APPEND);
                    } else {
                        file_put_contents('fcm_debug.txt', "Doubt $doubt_id | Token Empty or fcm_helper missing\n", FILE_APPEND);
                    }
                }
                $stmt_token->close();
            }
        }
        $st_stmt->close();
    } catch (Exception $e) { }

    /**
     * 6. FLASHCARD GENERATION: Create a study card for the student
     */
    try {
        $q_data = "SELECT student_id, description, subject FROM doubts WHERE id = ?";
        $d_stmt = $conn->prepare($q_data);
        $d_stmt->bind_param("i", $doubt_id);
        $d_stmt->execute();
        $d_res = $d_stmt->get_result()->fetch_assoc();

        if ($d_res) {
            $sid = $d_res['student_id'];
            $ques = $d_res['description'];
            $subj = $d_res['subject'];

            $f_sql = "INSERT INTO flashcards (student_id, doubt_id, subject, question, answer) VALUES (?, ?, ?, ?, ?)";
            $f_stmt = $conn->prepare($f_sql);
            $f_stmt->bind_param("iisss", $sid, $doubt_id, $subj, $ques, $solution);
            $f_stmt->execute();
            $f_stmt->close();
        }
        $d_stmt->close();
    } catch (Exception $e) { }

    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}

$stmt->close();
$conn->close();
?>