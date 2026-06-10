<?php
// Configuration for Database Connection
$host = "localhost";
$user = "YOUR_DB_USER";
$password = "YOUR_DB_PASSWORD";
$database = "YOUR_DB_NAME";
$port = 3306;

$conn = new mysqli($host, $user, $password, $database, $port);

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database Connection Failed"]));
}
?>