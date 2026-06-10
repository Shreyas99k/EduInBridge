<?php
// fetch_teachers.php
include 'db.php'; // Updated to match your file name

// Get the subject from the Android app's GET request
$subject = $_GET['subject'];

// Secure your query by using prepared statements to prevent SQL injection
// Added 'is_online' to the selection
$stmt = $conn->prepare("SELECT id, name, is_online FROM users WHERE role='teacher' AND expertise=?");
$stmt->bind_param("s", $subject);
$stmt->execute();
$result = $stmt->get_result();

$teachers = array();
while($row = $result->fetch_assoc()) {
    $teachers[] = $row;
}

// Return the data as a JSON array
echo json_encode($teachers);
?>