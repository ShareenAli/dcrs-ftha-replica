package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;

import java.util.Vector;
import javax.xml.ws.Endpoint;
import server.CompServer;
import server.InseServer;
import server.SoenServer;


public class replicaManager {
	
	private static String hostIP = "230.1.1.1"; // for host1: 230.1.1.1, host1: 230.1.1.2
	private static String coRM1_IP = "230.1.1.3";
	private static String coRM2_IP = "230.1.1.2";
	private static String FE_IP = "230.1.1.4"; // Front End IP
	private static String sequencerMessage;
	private static String messageOfFrontEnd;
	private static String messageOfServerResult;
	private static int UDP_port_comp = 8004;
	private static int UDP_port_soen = 8005;
	private static int UDP_port_inse = 8006;
	private static int RM_sequencer_port=1999; // need to be change according to sequencer
	private static int RM_port_for_sequencer=8007; // need to be change for each RM
	private static int RM_port_for_server=9001; // need to be change for each RM
	private static int RM_port_for_FE=7001; // need to be change for each RM
	private static int RM_port_for_heartBeat=9002; // need to be change for each RM for receiving results from dept Server, need to be change for each RM
	private static Vector<String> comp_operationSequence = new Vector<>();
	private static Vector<String> soen_operationSequence = new Vector<>();
	private static Vector<String> inse_operationSequence = new Vector<>();
	private static Vector<String> comp_operationResult = new Vector<>();
	private static Vector<String> soen_operationResult = new Vector<>();
	private static Vector<String> inse_operationResult = new Vector<>();
	private static Queue<String> comp_operationWaitingQueue = new LinkedList<>();
	private static Queue<String> soen_operationWaitingQueue = new LinkedList<>();
	private static Queue<String> inse_operationWaitingQueue = new LinkedList<>();
	private static int compResult_ERR_counter = 0;
	private static int soenResult_ERR_counter = 0;
	private static int inseResult_ERR_counter = 0;
	private static String requestServerName;
	private static int RM_to_comp_port = 9111;
	private static int RM_to_soen_port = 9112;
	private static int RM_to_inse_port = 9113;
	
	public static void main(String[] args) {			
		
		Runnable task = () -> {
			receiveMessage_SE();
		};
		Thread thread = new Thread(task);
		thread.start();
		
		Runnable task2 = () -> {
			receiveMessage_FE();
		};
		Thread thread2 = new Thread(task2);
		thread2.start();
		
		Runnable task3 = () -> {
			receiveMessage_SV();
		};
		Thread thread3 = new Thread(task3);
		thread3.start();
		
		Runnable task4 = () -> {
			receiveMessage_HB();
		};
		Thread thread4 = new Thread(task4);
		thread4.start();
		
		System.out.println("RM started!!!");
	}
	
    private static void sendMessage(String ipAddress, int serverPort, String messageContent) {
		 DatagramSocket aSocket = null; 	
			try{
				aSocket = new DatagramSocket(); //reference of the original socket
				byte [] message = messageContent.getBytes(); //message to be passed is stored in byte array
				//InetAddress aHost = InetAddress.getByName("localhost"); //Host name is specified and the IP address of server host is calculated using DNS. 
				InetAddress aHost = InetAddress.getByName(ipAddress);

				DatagramPacket request = new DatagramPacket(message, messageContent.length(), aHost, serverPort);//request packet ready
				aSocket.send(request);//request sent out
				//System.out.println("Request of client is : "+new String(request.getData()));
				
				//byte [] buffer = new byte[1000];//to store the received data, it will be populated by what receive method returns
				//DatagramPacket reply = new DatagramPacket(buffer, buffer.length);//reply packet ready but not populated.
				//Client waits until the reply is received-----------------------------------------------------------------------
				
				//aSocket.receive(reply);//reply received and will populate reply packet now.
				//System.out.println(" reply is: "+ new String(reply.getData()));//print reply message after converting it to a string from bytes
			}
			catch(SocketException e){
				System.out.println("Socket: "+e.getMessage());
			}
			catch(IOException e){
				e.printStackTrace();
				System.out.println("IO: "+e.getMessage());
			}
			finally{
				if(aSocket != null) aSocket.close();//now all resources used by the socket are returned to the OS, so that there is no
													//resource leakage, therefore, close the socket after it's use is completed to release resources.
			}
	 } 
    
    private static void receiveMessage_SE() {
    	MulticastSocket aSocket_Se = null;
		try {

			aSocket_Se = new MulticastSocket(RM_port_for_sequencer);
			aSocket_Se.joinGroup(InetAddress.getByName(hostIP));
			byte[] buffer_Se = new byte[1000];
			System.out.println("RM1 Started............");
			boolean proceed_flag = false;
			while (true) {
				// For receiving Sequencer message 
				DatagramPacket request_Se = new DatagramPacket(buffer_Se, buffer_Se.length);
				aSocket_Se.receive(request_Se);
				sequencerMessage = new String(request_Se.getData());
				String arg[] = sequencerMessage.split("-");
				requestServerName = arg[2].trim(); // need to be change according to sequencer
				String SeqNum = arg[0].trim();
				
				//String replyMessage = "From Comp Server-"+requestString.substring(0);
				//byte [] message = replyMessage.getBytes() ;			
				//DatagramPacket reply = new DatagramPacket(message, replyMessage.length(), request.getAddress(),
				//										  request.getPort());
				
				//String reply_SE_ack = SeqNum+"-"+requestServerName+"-acknowledgement";
				//DatagramPacket reply_Se = new DatagramPacket(request_Se.getData(), request_Se.getLength(), request_Se.getAddress(),
				//  request_Se.getPort());
				//aSocket_Se.send(reply_Se);
				proceed_flag = checkSequence(sequencerMessage);
				if(proceed_flag){
					System.out.println(sequencerMessage);
					System.out.println("Sequence of message is OK............");
				}else{
					System.out.println(sequencerMessage);
					System.out.println("Sequence of message is incorrect............");
				}
				// check if there is any waiting request in the queue of each dept server
				if(requestServerName.equalsIgnoreCase("comp")){
					if(comp_operationWaitingQueue.size()>0){
						String firstWaitingRequest = comp_operationWaitingQueue.peek();
						System.out.println("the comp waiting request is : "+firstWaitingRequest);
						String onHoldMessage[] = firstWaitingRequest.split("-");
						int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0].trim());
						if(onHoldMessageSeqNum == comp_operationSequence.size()+1){
							comp_operationSequence.add(firstWaitingRequest);
							comp_operationWaitingQueue.remove();
							System.out.println("Removed queue operation: "+firstWaitingRequest);
						}
					}
				}else if (requestServerName.equalsIgnoreCase("soen")){
					if(soen_operationWaitingQueue.size()>0){
						String firstWaitingRequest = soen_operationWaitingQueue.peek();
						System.out.println("the soen waiting request is : "+firstWaitingRequest);
						String onHoldMessage[] = firstWaitingRequest.split("-");
						int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0].trim());
						if(onHoldMessageSeqNum == soen_operationSequence.size()+1){
							soen_operationSequence.add(firstWaitingRequest);
							soen_operationWaitingQueue.remove();
						}
					}
				}else if (requestServerName.equalsIgnoreCase("inse")){
					if(inse_operationWaitingQueue.size()>0){
						String firstWaitingRequest = inse_operationWaitingQueue.peek();
						System.out.println("the inse waiting request is : "+firstWaitingRequest);
						String onHoldMessage[] = firstWaitingRequest.split("-");
						int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0].trim());
						if(onHoldMessageSeqNum == inse_operationSequence.size()+1){
							inse_operationSequence.add(firstWaitingRequest);
							inse_operationWaitingQueue.remove();
						}
					}
				}//==========end of checking queue======================//
				System.out.println("end of checking queue............");
			}

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket_Se != null)
				aSocket_Se.close();
		}
    }
    
    private static void receiveMessage_FE() {
    	MulticastSocket aSocket_FE = null;
		try {
			aSocket_FE = new MulticastSocket(RM_port_for_FE);
			aSocket_FE.joinGroup(InetAddress.getByName(hostIP));
			byte[] buffer_FE = new byte[1000];
			while (true) {
				// For receiving FE message
				DatagramPacket request_FE = new DatagramPacket(buffer_FE, buffer_FE.length);
				aSocket_FE.receive(request_FE);
				messageOfFrontEnd = new String(request_FE.getData());
				System.out.println("messageOfFrontEnd1: "+messageOfFrontEnd);
				if(!messageOfFrontEnd.isEmpty() && !messageOfFrontEnd.contains("Crashed")) {
					String arg1[] = messageOfFrontEnd.split("-");
					requestServerName = arg1[1].trim(); // need to be change according to sequencer
					System.out.println("messageOfFrontEnd2: "+messageOfFrontEnd);
					// check software error messages from FE for each dept server
					if(requestServerName.equalsIgnoreCase("comp")){				
						compResult_ERR_counter++;
						System.out.println("compResult_ERR_counter: "+compResult_ERR_counter);
						String errorMessage = "0-comp-error";
						sendMessage(hostIP, RM_to_comp_port, errorMessage);
						if(compResult_ERR_counter>2){
							System.out.println("compResult_ERR reaches to Max, need to check comp server ");
						}
					}else if (requestServerName.equalsIgnoreCase("soen")){
						soenResult_ERR_counter++;
						System.out.println("soenResult_ERR_counter: "+soenResult_ERR_counter);
						String errorMessage = "0-soen-error";
						sendMessage(hostIP, RM_to_soen_port, errorMessage);
						if(soenResult_ERR_counter>2){
							System.out.println("soenResult_ERR reaches to Max, need to check soen server  ");
						}
					}else if (requestServerName.equalsIgnoreCase("inse")){
						inseResult_ERR_counter++;
						System.out.println("inseResult_ERR_counter: "+inseResult_ERR_counter);
						String errorMessage = "0-inse-error";
						sendMessage(hostIP, RM_to_soen_port, errorMessage);
						if(inseResult_ERR_counter>2){
							System.out.println("inseResult_ERR reaches to Max, need to check inse server  ");
						}
					}
					System.out.println("end of checking FE message...........");
					// check system crash messages from FE for each dept server					
				}
				System.out.println("messageOfFrontEnd: "+messageOfFrontEnd);
				if(messageOfFrontEnd.contains("Crashed")){		
					System.out.println("Replica3 is crashed, restarting again.");
					comp_operationResult.clear();
					soen_operationResult.clear();
					inse_operationResult.clear();
					CompServer compServer = new CompServer();
					InseServer inseServer = new InseServer();
					SoenServer soenServer = new SoenServer();
					Endpoint endpoint1 = Endpoint.publish("http://localhost:9999/server/comp",compServer);
					Endpoint endpoint2 = Endpoint.publish("http://localhost:9999/server/soen", soenServer);
					Endpoint endpoint3 = Endpoint.publish("http://localhost:9999/server/inse", inseServer);
					sendMessage(hostIP, RM_to_comp_port, "0-sendMessageFalg-false");
					sendMessage(hostIP, RM_to_soen_port, "0-sendMessageFalg-false");
					sendMessage(hostIP, RM_to_inse_port, "0-sendMessageFalg-false");
					System.out.println("Replica3 starts to re-initiate servers.");
					for(int i=0;i<comp_operationSequence.size();i++){
						sendMessage(hostIP, RM_to_comp_port, comp_operationSequence.get(i));
					}
					for(int i=0;i<soen_operationSequence.size();i++){
						sendMessage(hostIP, RM_to_soen_port, soen_operationSequence.get(i));
					}
					for(int i=0;i<inse_operationSequence.size();i++){
						sendMessage(hostIP, RM_to_inse_port, inse_operationSequence.get(i));
					}
					System.out.println("Replica3 ends to re-initiate servers.");
					sendMessage(hostIP, RM_to_comp_port, "0-sendMessageFalg-true");
					sendMessage(hostIP, RM_to_soen_port, "0-sendMessageFalg-true");
					sendMessage(hostIP, RM_to_inse_port, "0-sendMessageFalg-true");
					
				}
				System.out.println("end of checking FE message for Crash...........");

			} // end of while

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket_FE != null)
				aSocket_FE.close();
		}
    }
    
    private static void receiveMessage_SV() {
    	MulticastSocket aSocket_Server = null;
		try {
			aSocket_Server = new MulticastSocket(RM_port_for_server);
			aSocket_Server.joinGroup(InetAddress.getByName(hostIP));
			byte[] buffer_Server = new byte[1000];
			while (true) {
				//  For receiving Server message
				DatagramPacket request_Server = new DatagramPacket(buffer_Server, buffer_Server.length);
				aSocket_Server.receive(request_Server);
				messageOfServerResult = new String(request_Server.getData());
				if(!messageOfServerResult.isEmpty()) {
					String arg2[] = messageOfServerResult.split("-");
					requestServerName = arg2[1].trim(); // need to be change according to sequencer
					// check result messages from Servers(each dept server) and save it to result vector
					if(requestServerName.equalsIgnoreCase("comp")){
						comp_operationResult.add(messageOfServerResult);
						System.out.println("Saving result from Comp Server: "+messageOfServerResult);
					}else if (requestServerName.equalsIgnoreCase("soen")){
						soen_operationResult.add(messageOfServerResult);
						System.out.println("Saving result from Soen Server: "+messageOfServerResult);
					}else if (requestServerName.equalsIgnoreCase("inse")){
						inse_operationResult.add(messageOfServerResult);
						System.out.println("Saving result from Inse Server: "+messageOfServerResult);
					}//==========end of checking queue======================//
				}
				System.out.println("end of checking server...........");
			}

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket_Server != null)
				aSocket_Server.close();
		}
    }
    
    private static void receiveMessage_HB() {
    	MulticastSocket aSocket_HB = null;
    	//DatagramSocket aSocket = null; // for UDP communication
		try {
			aSocket_HB = new MulticastSocket(RM_port_for_heartBeat);
			aSocket_HB.joinGroup(InetAddress.getByName(hostIP));
			byte[] buffer_HB = new byte[1000];
			while (true) {
				//  For receiving HeartBeat message
				DatagramPacket request_HB = new DatagramPacket(buffer_HB, buffer_HB.length);
				aSocket_HB.receive(request_HB);
				DatagramPacket reply_HB = new DatagramPacket(request_HB.getData(), request_HB.getLength(), request_HB.getAddress(),
						request_HB.getPort());
				aSocket_HB.send(reply_HB);
				//System.out.println("end of checking heart beat...........");
			}

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket_HB != null)
				aSocket_HB.close();
		}
    }
    
	public static boolean checkSequence(String inputMessage) {
		boolean validOp = false;
		String arg[] = inputMessage.split("-");
		String deptame = arg[2].trim(); // need to be change according to sequencer
		int sequenceNum = Integer.parseInt(arg[0]); // sequence number start from 1
		if(deptame.equalsIgnoreCase("comp")){
			if((sequenceNum-comp_operationSequence.size()-1)==0){
				comp_operationSequence.add(inputMessage);
				System.out.println("operation saved : " + inputMessage);
				System.out.println("comp_operationSequence.size() : " + comp_operationSequence.size());
				System.out.println("comp_operationWaitingQueue.size() : " + comp_operationWaitingQueue.size());
				validOp = true;
				if(comp_operationWaitingQueue.size()>0){
					String firstWaitingRequest = comp_operationWaitingQueue.peek();
					String onHoldMessage[] = firstWaitingRequest.split("-");
					int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0]);
					if(onHoldMessageSeqNum == sequenceNum){
						comp_operationWaitingQueue.remove();
					}
				}
			}else if(sequenceNum > comp_operationSequence.size()+1){
				comp_operationWaitingQueue.add(inputMessage);
				System.out.println("operation added in Queue : " + inputMessage);
			}
		}else if (deptame.equalsIgnoreCase("soen")){
			if((sequenceNum-soen_operationSequence.size()-1)==0){
				soen_operationSequence.add(inputMessage);
				validOp = true;
				if(soen_operationWaitingQueue.size()>0){
					String firstWaitingRequest = soen_operationWaitingQueue.peek();
					String onHoldMessage[] = firstWaitingRequest.split("-");
					int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0]);
					if(onHoldMessageSeqNum == sequenceNum){
						soen_operationWaitingQueue.remove();
					}
				}
			}else if(sequenceNum > soen_operationSequence.size()+1){
				soen_operationWaitingQueue.add(inputMessage);
			}
		}else if (deptame.equalsIgnoreCase("inse")){
			if((sequenceNum-inse_operationSequence.size()-1)==0){
				inse_operationSequence.add(inputMessage);
				validOp = true;
				if(inse_operationWaitingQueue.size()>0){
					String firstWaitingRequest = inse_operationWaitingQueue.peek();
					String onHoldMessage[] = firstWaitingRequest.split("-");
					int onHoldMessageSeqNum = Integer.parseInt(onHoldMessage[0]);
					if(onHoldMessageSeqNum == sequenceNum){
						inse_operationWaitingQueue.remove();
					}
				}
			}else if(sequenceNum > inse_operationSequence.size()+1){
				inse_operationWaitingQueue.add(inputMessage);
			}
		}
		return validOp;
	}
	
}
