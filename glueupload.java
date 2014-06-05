/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from local to remote.
 *   $ CLASSPATH=.:../build javac ScpTo.java
 *   $ CLASSPATH=.:../build java ScpTo file1 user@remotehost:file2
 * You will be asked passwd. 
 * If everything works fine, a local file 'file1' will copied to
 * 'file2' on 'remotehost'.
 *
 */
import com.jcraft.jsch.*;

import jBittorrentAPI.TorrentProcessor;

import java.awt.*;

import javax.swing.*;

import java.io.*;

/*
 * Address: ttv.crawf.org.nz
 * Username: ttv
 * Password: duhp1ej8ps
 * ttv@ttv.crawf.org.nz
 */

/**
 * This Code is a customized and merged version of both jBittorrentAPI and
 * jschAPI use samples.
 * References:
 * http://www.jcraft.com/jsch/examples/ScpTo.java.html
 * https://github.com/purplexcite/jBitTorrent-API/blob/master/ExampleCreateTorrent.java
 * 
 *
 */

public class glueupload{

	public static String generateTorrent(String filename,String announceURL, int pieceLengh,String creator,String comment) {
		TorrentProcessor tp = new TorrentProcessor();
		tp.setAnnounceURL(announceURL);


		tp.setPieceLength(pieceLengh);
		tp.addFile(filename);
		tp.setCreator(creator);
		tp.setComment(comment);

		//hashing the files
		try {
			System.out.println("Hashing the files");
			System.out.flush();
			tp.generatePieceHashes();
			String torrentFile = filename.substring(0,filename.lastIndexOf("."))+".torrent";
			FileOutputStream fos = new FileOutputStream(torrentFile);
			fos.write(tp.generateTorrent());
			System.out.println("Torrent Create Successfully");
			fos.close();
			return torrentFile; 
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println("ERROR: TORRENT COULD NOT BE CREATED!");
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	public static void main(String[] arg){
		if(arg.length!=1){
			System.err.println("usage: java ScpTo file1");
			System.exit(-1);
		}      

		FileInputStream fis=null;
		try{

			String lfile=glueupload.generateTorrent(arg[0], "http://ttv.crawf.org.nz/torrenttv/announce.php", 256, "TorrentTvTeam", "for problems please call to: 0800-HOLY-S**T");
			//lfile = null;
			if(lfile != null) {
				//String user=arg[1].substring(0, arg[1].indexOf('@'));
				//arg[1]=arg[1].substring(arg[1].indexOf('@')+1);
				//String host=arg[1].substring(0, arg[1].indexOf(':'));
				//String rfile=arg[1].substring(arg[1].indexOf(':')+1);

				JSch jsch=new JSch();
				Session session=jsch.getSession("ttv", "ttv.crawf.org.nz", 22);

				// username and password will be given via UserInfo interface.
				UserInfo ui=new MyUserInfo2();
				session.setUserInfo(ui);
				session.connect();

				boolean ptimestamp = true;

				// exec 'scp -t rfile' remotely
				String command="scp " + (ptimestamp ? "-p" :"") +" -t "+"/www/torrenttv/torrents;wine ConsoleUpload.exe Add "+lfile;
				Channel channel=session.openChannel("exec");				
				((ChannelExec)channel).setCommand(command);
				System.out.println("Command connection: "+command);
				// get I/O streams for remote scp
				OutputStream out=channel.getOutputStream();
				InputStream in=channel.getInputStream();

				channel.connect();

				if(checkAck(in)!=0){
					System.err.println("Channel didn't open!");
					System.exit(0);
				}

				File _lfile = new File(lfile);

				if(ptimestamp){
					command="T "+(_lfile.lastModified()/1000)+" 0";
					// The access time should be sent here,
					// but it is not accessible with JavaAPI ;-<
					command+=(" "+(_lfile.lastModified()/1000)+" 0\n"); 
					out.write(command.getBytes()); out.flush();
					System.out.println("Command for timestamp: "+command);
					if(checkAck(in)!=0){
						System.err.println("Timestamp command didn't work");
						System.exit(0);
					}
				}

				// send "C0644 filesize filename", where filename should not include '/'
				long filesize=_lfile.length();
				command="C0644 "+filesize+" ";
				if(lfile.lastIndexOf('/')>0){
					command+=lfile.substring(lfile.lastIndexOf('/')+1);
				}
				else{
					command+=lfile;
				}
				command+="\n";
				out.write(command.getBytes()); out.flush();
				System.out.println("Command for filename: "+command);
				if(checkAck(in)!=0){
					System.err.println("File Name didn't work");
					System.exit(0);
				}

				// send a content of lfile
				fis=new FileInputStream(lfile);
				byte[] buf=new byte[1024];
				while(true){
					int len=fis.read(buf, 0, buf.length);
					if(len<=0) break;
					out.write(buf, 0, len); //out.flush();
				}
				fis.close();
				fis=null;
				// send '\0'
				buf[0]=0; out.write(buf, 0, 1); out.flush();
				if(checkAck(in)!=0){
					System.err.println("Error while transfering the file");
					System.exit(0);
				}
				System.out.println("File uploaded successfully");
				
				
				
				out.close();
				channel.disconnect();
				session.disconnect();
				System.out.println("Done");
				System.exit(0);
				
			}
		}
		catch(Exception e){
			System.out.println(e);
			try{if(fis!=null)fis.close();}catch(Exception ee){}
		}
	}

	static int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if(b==0) return b;
		if(b==-1) return b;

		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {
				c=in.read();
				sb.append((char)c);
			}
			while(c!='\n');
			if(b==1){ // error
				System.out.print(sb.toString());
			}
			if(b==2){ // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	public static class MyUserInfo2 implements UserInfo {

		public String getPassphrase() {return null;}
		public String getPassword() {
			return "duhp1ej8ps";
		}
		public boolean promptPassphrase(String message) {return false;}

		public boolean promptPassword(String message) {return true;}

		public boolean promptYesNo(String message) {return true;}

		public void showMessage(String message){
			JOptionPane.showMessageDialog(null, message);
		}

	}
}