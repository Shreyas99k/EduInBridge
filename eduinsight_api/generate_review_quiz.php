<?php
header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 0);

try {
    include 'db.php';

    $user_id = $_GET['user_id'] ?? 0;
    if ($user_id <= 0) die(json_encode(["status" => "error", "message" => "Invalid User"]));

    // 1. Fetch the last 5 solved doubts for this student to create a personalized quiz
    $sql = "SELECT description, solution_text, subject FROM doubts
            WHERE student_id = ? AND status = 'solved'
            ORDER BY id DESC LIMIT 5";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $res = $stmt->get_result();

    $content_to_review = "";
    while ($row = $res->fetch_assoc()) {
        $content_to_review .= "Subject: " . $row['subject'] . "\nQuestion: " . $row['description'] . "\nSolution: " . $row['solution_text'] . "\n---\n";
    }
    $stmt->close();

    if (empty($content_to_review)) {
        echo json_encode(["status" => "error", "message" => "Not enough solved doubts for a quiz yet! Solve more doubts to unlock Weekly Review."]);
        exit();
    }

    // 2. Use Gemini AI to generate a quiz from this content
    $apiKey = "AIzaSyCPKZBljNe8tAiMJzsE6I853Vdx9q9m5u0"; // From your Config.java
    $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" . $apiKey;

    $prompt = "You are a helpful learning assistant. Based on the following solved doubts of a student, generate a 3-question Multiple Choice Quiz to help them review.
    Format the output as a PURE JSON array of objects. Each object must have: 'question', 'options' (array of 4), and 'correct_index' (0-3).
    Do not include any conversational text or markdown blocks, just the JSON array.

    Student Doubts Data:
    $content_to_review";

    $payload = [
        "contents" => [
            ["parts" => [["text" => $prompt]]]
        ]
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode == 200) {
        $result = json_decode($response, true);
        $ai_text = $result['candidates'][0]['content']['parts'][0]['text'];

        // Clean AI text in case it included ```json markdown
        $ai_text = str_replace(['```json', '```'], '', $ai_text);

        echo json_encode([
            "status" => "success",
            "quiz" => json_decode(trim($ai_text), true)
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "AI Generator busy. Try again later."]);
    }

} catch (Throwable $t) {
    echo json_encode(["status" => "error", "message" => $t->getMessage()]);
}
?>