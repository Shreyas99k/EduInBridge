<?php
header('Content-Type: application/json; charset=utf-8');

// Disable error reporting to prevent PHP warnings from breaking the JSON response
error_reporting(0);
ini_set('display_errors', 0);

// 1. Database Connection (Standard credentials)
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode([]));
}

if(isset($_GET['student_id'])) {
    $student_id = intval($_GET['student_id']);

    /**
     * UPDATED SQL: 
     * 1. Selects description, subject, status, is_mentorship, has_new_message, rating.
     * 2. Orders by latest message activity (mentorship chats or doubt creation).
     */
    $sql = "SELECT d.id, u.name as student_name, d.teacher_name, d.subject, 
                   d.description, d.status, d.solution_text as solution, 
                   d.is_mentorship, d.has_new_message, d.rating,
                   d.sol_image, d.sol_audio,
                   (SELECT message FROM mentorship_chats WHERE doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_msg_text,
                   (SELECT image_url FROM mentorship_chats WHERE doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_image,
                   (SELECT audio_url FROM mentorship_chats WHERE doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_audio,
                   (SELECT sender_role FROM mentorship_chats WHERE doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_sender_role,
                   (SELECT MAX(created_at) FROM mentorship_chats WHERE doubt_id = d.id) as last_message_time
            FROM doubts d 
            JOIN users u ON d.student_id = u.id 
            WHERE d.student_id = ? 
            ORDER BY COALESCE(last_message_time, d.created_at) DESC";

    $stmt = $conn->prepare($sql);
    
    if ($stmt) {
        $stmt->bind_param("i", $student_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $doubts = array();
        while($row = $result->fetch_assoc()) {
            $row['id'] = (int)$row['id'];
            $row['has_new_message'] = (int)($row['has_new_message'] ?? 0);

            // Snippet logic
            $last_msg = $row['last_msg_text'] ?? "";
            if (empty($last_msg)) {
                if (!empty($row['last_image'])) $last_msg = "📷 Image";
                else if (!empty($row['last_audio'])) $last_msg = "🎵 Audio";
                else $last_msg = $row['description'];
            }
            $row['last_message'] = $last_msg;

            // Only show unread indicator if the last message was NOT from the student
            if ($row['has_new_message'] == 1 && $row['last_sender_role'] === 'student') {
                $row['has_new_message'] = 0;
            }

            $doubts[] = $row;
        }

        // Return the clean JSON array
        echo json_encode($doubts);
        $stmt->close();
    } else {
        echo json_encode([]);
    }
} else {
    echo json_encode([]);
}

$conn->close();
?>