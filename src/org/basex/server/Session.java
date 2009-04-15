package org.basex.server;

import static org.basex.Text.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import org.basex.BaseX;
import org.basex.core.CommandParser;
import org.basex.core.Context;
import org.basex.core.Process;
import org.basex.core.Prop;
import org.basex.core.proc.Exit;
import org.basex.core.proc.GetInfo;
import org.basex.core.proc.GetResult;
import org.basex.io.BufferedOutput;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.util.Performance;

/**
 * Session for a Client Server Connection.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Andreas Weiler
 */
public class Session extends Thread {
  
  /** Database Context. */
  final Context context = new Context();
  /** Socket. */
  private Socket socket;
  /** ClientId. */
  private int clientId;
  /** Verbose mode. */
  boolean verbose;
  /** Core. */
  Process core;
  /** Flag for Session. */
  boolean running = true;
  /** Thread. */
  Thread thread;
  
  
  /**
   * Session.
   * @param s Socket
   * @param c ClientId
   * @param v Verbose Mode
   */
  public Session(final Socket s, final int c, final boolean v) {
    this.thread = new Thread("Session");
    this.clientId = c;
    this.socket = s;
    this.verbose = v;
  }
  
  /**
   * Handles Client Server Communication.
   * @throws IOException I/O Exception
   */
  private void handle() throws IOException {
    final Performance perf = new Performance();
    final InetAddress addr = socket.getInetAddress();
    final String ha = addr.getHostAddress();
    final int sp = socket.getPort();
    // get command and arguments
    DataInputStream dis = new DataInputStream(socket.getInputStream());
    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
    PrintOutput out = new PrintOutput(new BufferedOutput(
        socket.getOutputStream()));
    final int port = socket.getPort();
    String in;
    while (running) {
      in = getMessage(dis).trim(); 
      if(verbose) BaseX.outln("[%:%] %", ha, port, in);
      Process pr = null;
      try {
        pr = new CommandParser(in).parse()[0];
      } catch(final QueryException ex) {
        pr = new Process(0) { };
        pr.error(ex.extended());
        core = pr;
        send(-sp, dos);
        return;
      }
      if(pr instanceof Exit) {
        send(0, dos);
        // interrupt running processes
        running = false;
        break;
      }
      Process proc = pr;
      if(proc instanceof GetResult || proc instanceof GetInfo) {
        Process c = core;
        if(c == null) {
          out.print(BaseX.info(SERVERTIME, Prop.timeout));
        } else if(proc instanceof GetResult) {
          // the client requests result of the last process
          c.output(out);
        } else if(proc instanceof GetInfo) {
          // the client requests information about the last process
          c.info(out);
          out.write(0);
        }
        out.flush();
      } else {
        core = proc;
        send(proc.execute(context) ? sp : -sp, dos);
      }
      if(verbose) BaseX.outln("[%:%] %", ha, sp, perf.getTimer());
    }
    stopper();
  }
  
  /**
   * Returns the Message from the Client.
   * @param dis DataInputStream
   * @return String
   * @throws IOException I/O Exception
   */
  synchronized String getMessage(final DataInputStream dis) throws IOException {
    return dis.readUTF();
  }
  
  /**
   * Returns an answer to the client.
   * @param id session id to be returned
   * @param dos DataOutputStream
   * @throws IOException I/O exception
   */
  synchronized void send(final int id, final DataOutputStream dos)
  throws IOException {
    dos.writeInt(id);
    dos.flush();
    }
  
  /**
   * Stops the thread. 
   */
  synchronized void stopper() {
    BaseX.outln("Client " + clientId + " has logged out.");
    try {
      socket.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
    thread = null;
  }
  
  @Override
  public void run() {
    try {
      handle(); 
    } catch(Exception io) {
      // for forced stops
      if (io instanceof SocketException) {
       stopper();
      } else {
      io.printStackTrace();
      }
    }
  }
}