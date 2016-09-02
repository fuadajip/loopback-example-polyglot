package com.ibm.apiconnect.demo.polyglot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Server that manages startup/shutdown of a {@code NoteService} server.
 */
public class NoteServer {
	private static final Logger logger = Logger.getLogger(NoteServer.class.getName());

	/* The port on which the server should run */
	private int port = 50051;
	private Server server;

	private void start() throws IOException {
		server = ServerBuilder.forPort(port).addService(new NoteServiceImpl()).build().start();
		logger.info("Server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its
				// JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				NoteServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	/**
	 * Main launches the server from the command line.
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final NoteServer server = new NoteServer();
		server.start();
		server.blockUntilShutdown();
	}

	private static List<Note> notes = new ArrayList<Note>();
	private static volatile int index = 0;

	private class NoteServiceImpl extends NoteServiceGrpc.NoteServiceImplBase {

		@Override
		public void create(Note req, StreamObserver<Note> responseObserver) {
			Note reply = Note.newBuilder(req).setId(++index).build();
			notes.add(reply);
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		public void findById(com.ibm.apiconnect.demo.polyglot.FindByIdRequest request,
				io.grpc.stub.StreamObserver<com.ibm.apiconnect.demo.polyglot.Note> responseObserver) {
			Note found = null;
			for (Note n : notes) {
				if (n.getId() == request.getId()) {
					found = n;
					break;
				}
			}
			responseObserver.onNext(found);
			responseObserver.onCompleted();
		}

		/**
		 */
		public void find(com.ibm.apiconnect.demo.polyglot.FindRequest request,
				io.grpc.stub.StreamObserver<com.ibm.apiconnect.demo.polyglot.FindResponse> responseObserver) {
			FindResponse reply = FindResponse.newBuilder().addAllNotes(notes).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
	}
}