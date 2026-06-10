<?php
header('Content-Type: application/json');
error_reporting(0);
ini_set('display_errors', 0);

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

try {
    include 'db.php';

    $identity = $_POST['identity'] ?? '';
    $otp = $_POST['otp'] ?? '';

    if (empty($identity) || empty($otp)) {
        die(json_encode(["status" => "error", "message" => "Missing data"]));
    }

    require 'PHPMailer/src/Exception.php';
    require 'PHPMailer/src/PHPMailer.php';
    require 'PHPMailer/src/SMTP.php';

    $mail = new PHPMailer(true);

    // SMTP Settings - REVERTED EXACTLY TO YOUR OLD METHOD
    $mail->isSMTP();
    $mail->Host       = 'smtp.gmail.com';
    $mail->SMTPAuth   = true;
    $mail->Username   = 'YOUR_EMAIL@gmail.com';
    $mail->Password   = 'YOUR_APP_PASSWORD';
    $mail->SMTPSecure = 'tls';
    $mail->Port       = 587;

    // SSL Bypass for XAMPP
    $mail->SMTPOptions = array(
        'ssl' => array(
            'verify_peer' => false,
            'verify_peer_name' => false,
            'allow_self_signed' => true
        )
    );

    $mail->setFrom('YOUR_EMAIL@gmail.com', 'EduInBridge');
    $mail->addAddress($identity);
    $mail->isHTML(true);
    $mail->Subject = 'Your Verification Code';
    $mail->Body    = "<h3>Your code is: <b>$otp</b></h3>";

    $mail->send();

    echo json_encode(["status" => "success", "message" => "OTP sent"]);

} catch (Exception $e) {
    file_put_contents('email_log.txt', date('Y-m-d H:i:s') . " | To: $identity | Error: " . $mail->ErrorInfo . "\n", FILE_APPEND);
    // Allow user to proceed with debug code if mail still fails
    echo json_encode(["status" => "success", "message" => "Mail service blocked. Use: $otp", "error" => $mail->ErrorInfo]);
} catch (Throwable $t) {
    echo json_encode(["status" => "error", "message" => $t->getMessage()]);
}
?>