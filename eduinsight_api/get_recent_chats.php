<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed"]));
}

$teacher_name = $_GET['teacher_name'] ?? '';

if (empty($teacher_name)) {
    echo json_encode(["status" => "error", "message" => "Teacher name required", "chats" => []]);
    exit();
}

// Query: Find mentorship sessions for the teacher, showing latest activity first
// MODIFIED: Only include chats where at least one message from a student exists
$sql = "SELECT d.*,
        (SELECT MAX(created_at) FROM mentorship_chats m WHERE m.doubt_id = d.id) as last_msg_time,
        (SELECT message FROM mentorship_chats m4 WHERE m4.doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_message_text,
        (SELECT image_url FROM mentorship_chats m5 WHERE m5.doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_image,
        (SELECT audio_url FROM mentorship_chats m6 WHERE m6.doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_audio,
        (SELECT sender_role FROM mentorship_chats m7 WHERE m7.doubt_id = d.id ORDER BY created_at DESC LIMIT 1) as last_sender_role,
        (SELECT COUNT(*) FROM mentorship_chats m3 WHERE m3.doubt_id = d.id AND m3.sender_role = 'teacher') as teacher_replied_count
        FROM doubts d 
        WHERE d.teacher_name = ? AND d.is_mentorship = 1
        AND EXISTS (SELECT 1 FROM mentorship_chats m8 WHERE m8.doubt_id = d.id AND m8.sender_role = 'student')
        ORDER BY COALESCE(last_msg_time, d.created_at) DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $teacher_name);
$stmt->execute();
$result = $stmt->get_result();

$chats = [];
while($row = $result->fetch_assoc()) {
    $row['id'] = (int)$row['id'];
    $row['teacher_replied_count'] = (int)$row['teacher_replied_count'];
    $row['has_new_message'] = (int)($row['has_new_message'] ?? 0);

    // Determine the last message snippet
    $last_msg = $row['last_message_text'] ?? "";
    if (empty($last_msg)) {
        if (!empty($row['last_image'])) $last_msg = "📷 Image";
        else if (!empty($row['last_audio'])) $last_msg = "🎵 Audio";
        else $last_msg = $row['description']; // Fallback to doubt description
    }
    $row['last_message'] = $last_msg;

    // Only show unread indicator if the last message was NOT from the teacher
    if ($row['has_new_message'] == 1 && $row['last_sender_role'] === 'teacher') {
        $row['has_new_message'] = 0;
    }

    $chats[] = $row;
}

echo json_encode(["status" => "success", "chats" => $chats]);
$stmt->close();
$conn->close();
?>