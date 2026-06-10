<?php
header('Content-Type: application/json');

// 1. Database Connection
// Ensure these credentials match your local MySQL setup (standard is "" for password, but your login.php used "8431")
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Fetch all users who are registered as Institutions
$sql = "SELECT id, name FROM users WHERE role = 'institution' ORDER BY name ASC";
$result = $conn->query($sql);

$institutions = array();

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $institutions[] = array(
            "id" => $row["id"],
            "name" => $row["name"]
        );
    }
}

// 3. Return the array directly (as expected by the Android app's JSONArray parser)
echo json_encode($institutions);

$conn->close();
?>
