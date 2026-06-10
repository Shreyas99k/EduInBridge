<?php
header('Content-Type: application/json');
include 'db.php';

$inst_id = $_GET['institution_id'] ?? '';
$subject = $_GET['subject'] ?? '';
$query   = $_GET['query'] ?? '';

// DEBUG LOG
$log = date('Y-m-d H:i:s') . " | Request: Inst=$inst_id, Subj=$subject, Query=$query\n";

if (empty($subject)) {
    file_put_contents('rec_debug.txt', $log . "Result: Empty Subject\n", FILE_APPEND);
    echo json_encode([]);
    exit();
}

try {
    // Loosen search: Match anything that looks like the subject or query in name or subject
    $s = "%" . trim($subject) . "%";
    $q = "%" . trim($query) . "%";

    // We search GLOBALLY first to ensure user sees SOMETHING if their institution is empty
    $sql = "SELECT m.*, u.name as uploader_name
            FROM materials m
            LEFT JOIN users u ON m.uploader_id = u.id
            WHERE (m.subject_name LIKE ? OR m.file_name LIKE ? OR m.subject_name LIKE ? OR m.file_name LIKE ?)
            ORDER BY m.id DESC LIMIT 3";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ssss", $s, $s, $q, $q);
    $stmt->execute();
    $result = $stmt->get_result();

    $recommendations = [];
    while ($row = $result->fetch_assoc()) {
        $recommendations[] = $row;
    }

    file_put_contents('rec_debug.txt', $log . "Result: Found " . count($recommendations) . " items\n", FILE_APPEND);
    echo json_encode($recommendations);
    $stmt->close();
} catch (Exception $e) {
    file_put_contents('rec_debug.txt', $log . "Error: " . $e->getMessage() . "\n", FILE_APPEND);
    echo json_encode([]);
}

$conn->close();
?>