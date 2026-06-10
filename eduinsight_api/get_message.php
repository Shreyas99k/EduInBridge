<?php
header('Content-Type: application/json; charset=utf-8');
ini_set('display_errors', 0);
error_reporting(E_ALL);

try {
    include 'db.php';

    $doubt_id = isset($_GET['doubt_id']) ? intval($_GET['doubt_id']) : 0;
    $viewer_role = isset($_GET['viewer_role']) ? strtolower(trim($_GET['viewer_role'])) : '';

    if ($doubt_id <= 0) {
        echo json_encode(array());
        exit();
    }

    $sql = "SELECT id, message, image_url, audio_url, sender_role AS sender_type, created_at,
                   reply_to_message, reply_to_user, is_deleted, is_edited, deleted_by
            FROM mentorship_chats
            WHERE doubt_id = ?
            ORDER BY created_at ASC";

    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        die(json_encode(array("error" => "db_error: " . $conn->error)));
    }

    $stmt->bind_param("i", $doubt_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $messages = array();
    $last_sender_role = '';
    $viewer_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : -1;

    while ($row = $result->fetch_assoc()) {
        if ($row['deleted_by'] != null && $row['deleted_by'] == $viewer_id) {
            continue;
        }

        $last_sender_role = strtolower($row['sender_type']);
        $messages[] = array(
            "id"               => (int)$row['id'],
            "message"          => $row['message'] ? $row['message'] : "",
            "image_url"        => $row['image_url'] ? $row['image_url'] : "",
            "audio_url"        => $row['audio_url'] ? $row['audio_url'] : "",
            "sender_type"      => $last_sender_role,
            "created_at"       => date("h:i A", strtotime($row['created_at'])),
            "reply_to_message" => $row['reply_to_message'],
            "reply_to_user"    => $row['reply_to_user'],
            "is_saved"         => isset($row['is_saved']) ? (int)$row['is_saved'] : 0,
            "is_deleted"       => (int)$row['is_deleted'],
            "is_edited"        => (int)$row['is_edited'],
            "deleted_by"       => (int)$row['deleted_by']
        );
    }

    if (!empty($viewer_role) && $last_sender_role !== $viewer_role) {
        $conn->query("UPDATE doubts SET has_new_message = 0 WHERE id = $doubt_id");
    }

    echo json_encode($messages);

    $stmt->close();
    $conn->close();

} catch (Throwable $t) {
    echo json_encode(array("status" => "error", "message" => $t->getMessage()));
}
?>