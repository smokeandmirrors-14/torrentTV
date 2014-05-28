<html>
<head>
<link rel="stylesheet" href="style.css" type="text/css" media="screen, projection" />
</head>

<center>
<?php
echo "<h1> WELCOME TO TORRENT TV! </h1>";
echo "<p> Tracker annouce url:<br><b>http://ttv.crawf.org.nz/torrenttv/announce.php</b><br></p>";
$con=mysqli_connect("localhost","ttv","duhp1ej8ps","tracker");
if (mysqli_connect_errno()) {
echo "Failed to connect to MySQL: " . mysql_connect_error();
}

echo "<h2> Torrent Summary </h2>";
$result = mysqli_query($con,"SELECT * FROM summary, namemap WHERE summary.info_hash = namemap.info_hash");

echo "<table border='1'>
<tr>
<th>Torrent Name</th>
<th>Torrent Hash</th>
<th>Seeders</th>
<th>Leechers</th>
</tr>";

while($row = mysqli_fetch_array($result)) {
  echo "<tr>";
  echo "<td>" . $row['filename'] . "</td>";
  echo "<td>" . $row['info_hash'] . "</td>";
  echo "<td>" . $row['seeds'] . "</td>";
  echo "<td>" . $row['leechers'] . "</td>";
  echo "</tr>";
}

echo "</table>";

echo "<h2>Connected Peers</h2>";
$result = mysqli_query($con,"SELECT * FROM peers");
echo "<table border='1'>
<tr>
<th>Torrent Hash</th>
<th>Peer ID</th>
<th>IP Address</th>
<th>Port</th>
<th>Status</th>
</tr>";

while($row = mysqli_fetch_array($result)) {
  echo "<tr>";
  echo "<td>" . $row['infohash'] . "</td>";
  echo "<td>" . $row['peer_id'] . "</td>";
  echo "<td>" . $row['ip'] . "</td>";
  echo "<td>" . $row['port'] . "</td>";
  echo "<td>" . $row['status'] . "</td>";
  echo "</tr>";
}

echo "</table>";
echo "<p>&nbsp</p>";

mysqli_close($con);

?>

<?php

require_once ("include/functions.php");
require_once ("include/config.php");
require_once ("include/BDecode.php");
require_once ("include/BEncode.php");

//// Configuration//
function_exists("sha1") or die('<FONT COLOR="red">".NOT_SHA."</FONT></BODY></HTML>');

dbconn();

if (!$CURUSER || $CURUSER["can_upload"]=="no")
   {
       stderr(ERROR.NOT_AUTHORIZED_UPLOAD,SORRY."...");
   }

if (isset($_FILES["torrent"]))
   {
   if ($_FILES["torrent"]["error"] != 4)
   {
      $fd = fopen($_FILES["torrent"]["tmp_name"], "rb") or die(FILE_UPLOAD_ERROR_1);
      is_uploaded_file($_FILES["torrent"]["tmp_name"]) or die(FILE_UPLOAD_ERROR_2);
      $length=filesize($_FILES["torrent"]["tmp_name"]);
      if ($length)
        $alltorrent = fread($fd, $length);
      else {
        err_msg(ERROR,FILE_UPLOAD_ERROR_3);
        exit();

       }
      $array = BDecode($alltorrent);
      if (!isset($array))
         {
         echo '<FONT COLOR="red">Invalid file. <br>Only upload .torrent file<br></FONT>';
         endOutput();
         exit;
         }
      if (!$array)
         {
         echo '<FONT COLOR="red">Invalid file. <br>Only upload .torrent file<br></FONT>';
         endOutput();
         exit;
         }
    // if dht disabled ($DHT_PRIVATE=true), set private flag and save new info hash
    //if ($array["announce"]==$BASEURL."/announce.php" && $DHT_PRIVATE)
    if (in_array($array["announce"],$TRACKER_ANNOUNCEURLS) && $DHT_PRIVATE)
      {
      $array["info"]["private"]=1;
      $hash=sha1(BEncode($array["info"]));
      }
    else
      {
      $hash = sha1(BEncode($array["info"]));
      }
      fclose($fd);
      }

if (isset($_POST["filename"]))
   $filename=mysql_escape_string(htmlspecialchars($_POST["filename"]));
else
    $filename = mysql_escape_string(htmlspecialchars($_FILES["torrent"]["name"]));

if (isset($hash) && $hash) $url = $TORRENTSDIR . "/" . $hash . ".btf";
else $url = 0;

if (strlen($filename) == 0 && isset($array["info"]["name"]))
   $filename = mysql_escape_string(htmlspecialchars($array["info"]["name"]));

if (isset($array["comment"]))
   $info = mysql_escape_string($array["comment"]);
else
    $info = "";

if (isset($array["info"]) && $array["info"]) $upfile=$array["info"];
    else $upfile = 0;

if (isset($upfile["length"]))
{
  $size = floatval($upfile["length"]);
}
else if (isset($upfile["files"]))
     {
// multifiles torrent
         $size=0;
         foreach ($upfile["files"] as $file)
                 {
                 $size+=floatval($file["length"]);
                 }
     }
else
    $size = "0";

if (!isset($array["announce"]))
     {
     err_msg(ERROR, "Announce is empty");
     print("</td></tr></table>");
     exit();
}

      $categoria = intval(0+$_POST["category"]);
      $announce=$array["announce"];
      $anonyme=sqlesc($_POST["anonymous"]);
      $curuid=intval($CURUSER["uid"]);


      if ((strlen($hash) != 40) || !verifyHash($hash))
      {
         echo("<center><FONT COLOR=\"red\">".ERR_HASH."</FONT></center>");
         endOutput();
      }
//      if ($announce!=$BASEURL."/announce.php" && $EXTERNAL_TORRENTS==false)
      if (!in_array($announce,$TRACKER_ANNOUNCEURLS) && $EXTERNAL_TORRENTS==false)
         {
           err_msg(ERROR,ERR_EXTERNAL_NOT_ALLOWED);
           unlink($_FILES["torrent"]["tmp_name"]);
           print("</td></tr></table>");
           exit();
         }
//      if ($announce!=$BASEURL."/announce.php")
      if (in_array($announce,$TRACKER_ANNOUNCEURLS))
         $query = "INSERT INTO namemap (info_hash, filename, url, info, category, data, size, comment, uploader,anonymous) VALUES (\"$hash\", \"$filename\", \"$url\", \"$info\",0 + $categoria,NOW(), \"$size\", \"$comment\",".$curuid.",$anonyme)";
      else
         $query = "INSERT INTO namemap (info_hash, filename, url, info, category, data, size, comment,external,announce_url, uploader,anonymous) VALUES (\"$hash\", \"$filename\", \"$url\", \"$info\",0 + $categoria,NOW(), \"$size\", \"$comment\",\"yes\",\"$announce\",".$curuid.",$anonyme)";

      //echo $query;
      $status = makeTorrent($hash, true);
      quickQuery($query);
      if ($status)
         {
         move_uploaded_file($_FILES["torrent"]["tmp_name"] , $TORRENTSDIR . "/" . $hash . ".btf") or die(ERR_MOVING_TORR);
//         if ($announce!=$BASEURL."/announce.php")
        if (!in_array($announce,$TRACKER_ANNOUNCEURLS))
            {
                require_once("./include/getscrape.php");
                scrape($announce,$hash);
                print("<center>".UPLOADED."<br /><br />\n");
                write_log("Uploaded new torrent $filename - EXT ($hash)","add");
            }
         else
             {
              if ($DHT_PRIVATE)
                   {
                   $alltorrent=bencode($array);
                   $fd = fopen($TORRENTSDIR . "/" . $hash . ".btf", "rb+");
                   fwrite($fd,$alltorrent);
                   fclose($fd);
                   }
                // with pid system active or private flag (dht disabled), tell the user to download the new torrent
                write_log("Uploaded new torrent $filename ($hash)","add");
                //print("<center>".UPLOADED."<br /><br />\n");
                echo '<center> Successfully Uploaded! </center>';
                if ($PRIVATE_ANNOUNCE || $DHT_PRIVATE)
                   print(MSG_DOWNLOAD_PID."<br /><a href=\"download.php?id=$hash&f=".urlencode($filename).".torrent\">".DOWNLOAD."</a><br /><br />");
             }
             echo '<a href="index.php">Continue</a></center>';
         }
      else
          {
            echo "File already exists!";
            endOutput();
            exit;
          }
}
else
    endOutput();

function endOutput()
{
 global $BASEURL, $user_id, $TRACKER_ANNOUNCEURLS; ?>

  <?php echo "<h2> Upload .torrent file </h2>"; ?>
  <FORM name="upload" method="post" ENCTYPE="multipart/form-data">
  <?php echo ".Torrent File"; ?>
  <?php
  if (function_exists("sha1"))
     echo '<INPUT TYPE="file" NAME="torrent">';
  else
      echo "<I>".NO_SHA_NO_UP."</I>";
  ?>
  <?php
  if (function_exists("sha1"))
     echo '<INPUT type="checkbox" name="autoset" value="enabled" disabled checked />'.VERIFY.'';
  ?>
  <INPUT type="submit" value="<?php echo Upload ?>" />
  </FORM>
  <?php
}
?>

