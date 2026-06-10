<?php
include 'db.php';

$sql = "SELECT name FROM users WHERE role = 'teacher'";
$result = $conn->query($sql);

$teachers = array();
while($row = $result->fetch_assoc()) {
    $teachers[] = $row['name'];
}

echo json_encode($teachers);
?>