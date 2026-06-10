<?php
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");
$action = $_POST['action']; // 'start' or 'end'
$doubt_id = $_POST['doubt_id'];

if ($action == 'start') {
    $room = "EduInsight_Doubt_" . $doubt_id . "_" . bin2hex(random_bytes(4));
    $conn->query("UPDATE doubts SET call_room = '$room' WHERE id = '$doubt_id'");
    echo json_encode(["status" => "success", "room" => $room]);
} else {
    $conn->query("UPDATE doubts SET call_room = NULL WHERE id = '$doubt_id'");
    echo "success";
}
$conn->close();
?>