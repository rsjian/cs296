#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <time.h>

#define MAX_CLIENTS 8
#define BUFFER_SIZE 128
#define REQUEST_GAME_MESG "/r"
#define CONFIRM_REQUEST_MESG "/a"
#define PASS -1
#define RESIGN -2

static JavaVM *jvm;

jint JNI_OnLoad (JavaVM* vm, void* reserved) {
	// Cache the JavaVM interface pointer
	jvm = vm;
    return JNI_VERSION_1_4;
}


void* print(void* arg) {
	while(1) {
		__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "printing");
		sleep(1);
	}
}

pthread_t server_tid;
jint server_port;
int client_count;
int client_fds[MAX_CLIENTS];
pthread_t client_tids[MAX_CLIENTS];
static jobject server_obj;

int server_fd;
pthread_t client_tid;
jint client_port;
const char* client_ip;
static jobject client_obj;

int board_size;
int komi;

int game_started = 0;
int my_turn;
jint move_idx = -1;
int waiting = 1;
int passes = 0;

pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cv = PTHREAD_COND_INITIALIZER;

void gameLoop(JNIEnv* env, jclass cls, jobject obj, int fd, int color) {
	char buffer[BUFFER_SIZE];
	int n;
	jmethodID instanceMethodId;
	while(1) {
		if(!color) {
			//wait for user to play stone.
			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "waiting for UI");
			pthread_mutex_lock(&m);
			waiting = 1;
			while(waiting) {
				pthread_cond_wait(&cv, &m);
			} pthread_mutex_unlock(&m);

			// notify client of move
			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "sent:%d", move_idx);
			int mesg_len = sprintf(buffer, "%d", move_idx);
			write(fd, buffer, mesg_len);
			color ^= 1;
			if(move_idx == PASS) {
				__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "passed");
				if(++passes == 2) {
					__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "2 passes");
					instanceMethodId = (*env)->GetMethodID(env, cls, "endGame", "()V");
					(*env)->CallVoidMethod(env, obj, instanceMethodId);
					break;
				}
			} else {
				passes = 0;
			}
			if(move_idx == RESIGN) {
				break;
			}
		}
		if(color) {
			// wait for client move
			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "waiting for opponent");
			n = read(fd, buffer, BUFFER_SIZE - 1);
			buffer[n] = '\0';
			sscanf(buffer, "%d", &move_idx);

			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "received: %d", move_idx);
			// notify UI to display client's move
			instanceMethodId = (*env)->GetMethodID(env, cls, "playMove", "(I)V");
			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "got playMove methodID");
			(*env)->CallVoidMethod(env, obj, instanceMethodId, move_idx);
			__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "after callvoidmethod");
			color ^= 1;
			if(move_idx == PASS) {
				__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "passed");
				if(++passes == 2) {
					__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "2 passes");
					instanceMethodId = (*env)->GetMethodID(env, cls, "endGame", "()V");
					(*env)->CallVoidMethod(env, obj, instanceMethodId);
					break;
				}
			} else {
				passes = 0;
			}
			if(move_idx == RESIGN) {
				break;
			}
		}
	}
}

void* serveClient(void* arg) {
	JNIEnv* env;
	(*jvm)->AttachCurrentThread(jvm, &env, NULL);
	int client_idx = (int)arg;

	int n;
	char buffer[BUFFER_SIZE];
	//write(client_fds[client_idx], "You have connected", strlen("You have connected"));

	jclass cls = (*env)->GetObjectClass(env, server_obj);
	jfieldID ifd = (*env)->GetFieldID(env, cls, "boardSize", "I");
	board_size = (*env)->GetIntField(env, server_obj, ifd);

	ifd = (*env)->GetFieldID(env, cls, "komi", "I");
	komi = (*env)->GetIntField(env, server_obj, ifd);

	//send client game info
	int mesg_len = sprintf(buffer, "%d,%d\n", board_size, komi);
	write(client_fds[client_idx], buffer, mesg_len);
	__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "sent: %s", buffer);

	// read client game request
	n = read(client_fds[client_idx], buffer, BUFFER_SIZE - 1);
	buffer[n] = '\0';
	__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "read client game request: %s", buffer);

	if(!strcmp(buffer, REQUEST_GAME_MESG)) {
		// notify UI to allow user to confirm request
		jmethodID instanceMethodId = (*env)->GetMethodID(env, cls, "enableStart", "()V");
		(*env)->CallVoidMethod(env, server_obj, instanceMethodId);

		__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "waiting for UI confirmation");
		//wait for user to confirm request
		pthread_mutex_lock(&m);
		while(game_started == 0) {
			pthread_cond_wait(&cv, &m);
		} pthread_mutex_unlock(&m);

		//notify client of confirmation
		write(client_fds[client_idx], CONFIRM_REQUEST_MESG, strlen(CONFIRM_REQUEST_MESG));

		srand(time(NULL));
		int color = rand() % 2;
		mesg_len = sprintf(buffer, "%d", color ^ 1);
		write(client_fds[client_idx], buffer, mesg_len);

		// notify UI to switch to game view
		instanceMethodId = (*env)->GetMethodID(env, cls, "startGame", "(I)V");
		(*env)->CallVoidMethod(env, server_obj, instanceMethodId, color);

		gameLoop(env, cls, server_obj, client_fds[client_idx], color);
	}

	while((n = read(client_fds[client_idx], buffer, BUFFER_SIZE - 1)) > 0) {
		buffer[n]  = '\0';
		__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "Server: %s", buffer);
	}
	(*jvm)->DetachCurrentThread(jvm);
	return NULL;
}

void* serverThread(void* arg) {
	JNIEnv* env;
	(*jvm)->AttachCurrentThread(jvm, &env, NULL);

	int server_socket = socket(PF_INET, SOCK_STREAM, 0);
	struct sockaddr_in address;
	memset(&address, 0, sizeof(address));
	address.sin_family = PF_INET;
	//address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
	address.sin_port = htons(server_port);

	setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, (struct sockaddr*) &address, sizeof(address));
	bind(server_socket, (struct sockaddr*) &address, sizeof(address));
	listen(server_socket, MAX_CLIENTS);

	struct sockaddr_in address2;
	socklen_t addressLength = sizeof(address2);

	while((client_fds[client_count] = accept(server_socket, (struct sockaddr*) &address2, &addressLength)) != -1) {
		pthread_create(client_tids + client_count, NULL, serveClient, (void*)client_count);
		client_count++;
	}
	(*jvm)->DetachCurrentThread(jvm);
	return NULL;
}

void
Java_com_rsjian2_cs296_ServerActivity_startServerThread(
	JNIEnv* env,
	jobject obj,
	jint port)
{
	game_started = 0;
	server_port = port;
	client_count = 0;
	server_obj = (*env)->NewGlobalRef(env, obj);
	pthread_create(&server_tid, NULL, serverThread, NULL);
}

void* clientThread(void* arg) {
	JNIEnv* env;
	(*jvm)->AttachCurrentThread(jvm, &env, NULL);
	server_fd = socket(PF_INET, SOCK_STREAM, 0);

	struct sockaddr_in address;
	memset(&address, 0, sizeof(address));
	address.sin_family = PF_INET;

	inet_aton(client_ip, &(address.sin_addr));
	address.sin_port = htons(client_port);
	connect(server_fd, (struct sockaddr*) &address, sizeof(address));

	char buffer[BUFFER_SIZE];
	int n;

	//read game infomation
	n = read(server_fd, buffer, BUFFER_SIZE - 1);
	buffer[n] = '\0';
	sscanf(buffer, "%d,%d\n", &board_size, &komi);
	__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "%d %d", board_size, komi);

	//notify clientActivity
	jclass cls = (*env)->GetObjectClass(env, client_obj);
	jmethodID instanceMethodId = (*env)->GetMethodID(env, cls, "loadGameInfo", "(II)V");
	(*env)->CallVoidMethod(env, client_obj, instanceMethodId, board_size, komi);



	//after client sends game request
	//wait for server to confirm request
	n = read(server_fd, buffer, BUFFER_SIZE - 1);
	buffer[n] = '\0';
	if(!strcmp(buffer, CONFIRM_REQUEST_MESG)) {
		//get color from server
		n = read(server_fd, buffer, BUFFER_SIZE - 1);
		buffer[n] = '\0';
		int color;
		sscanf(buffer, "%d", &color);

		// tell UI to switch to game
		instanceMethodId = (*env)->GetMethodID(env, cls, "startGame", "(I)V");
		(*env)->CallVoidMethod(env, client_obj, instanceMethodId, color);

		gameLoop(env, cls, client_obj, server_fd, color);
	}
	while((n = read(server_fd, buffer, BUFFER_SIZE - 1)) > 0) {
		buffer[n] = '\0';
		__android_log_print(ANDROID_LOG_VERBOSE, "asdf", "Client: %s", buffer);
	}

	(*jvm)->DetachCurrentThread(jvm);
	return NULL;
}

JNIEXPORT void JNICALL
Java_com_rsjian2_cs296_ClientActivity_startClientThread(
	JNIEnv* env,
	jobject obj,
	jstring ip,
	jint port)
{
	client_port = port;
	client_ip = (*env)->GetStringUTFChars(env, ip, NULL);
	client_obj =  (*env)->NewGlobalRef(env, obj);

	pthread_create(&client_tid, NULL, clientThread, NULL);

	//(*env)->ReleaseStringUTFChars(env, ip, ipAddress);
}

JNIEXPORT void JNICALL
Java_com_rsjian2_cs296_ClientActivity_requestGame(
	JNIEnv* env,
	jobject obj)
{
	write(server_fd, REQUEST_GAME_MESG, strlen(REQUEST_GAME_MESG));
}

JNIEXPORT void JNICALL
Java_com_rsjian2_cs296_ServerActivity_confirmGame(
	JNIEnv* env,
	jobject obj)
{
	pthread_mutex_lock(&m);
	game_started = 1;
	pthread_cond_signal(&cv);
	pthread_mutex_unlock(&m);
}

void notifyMovePlayed(int idx) {
	pthread_mutex_lock(&m);
	waiting = 0;
	move_idx = idx;
	pthread_cond_signal(&cv);
	pthread_mutex_unlock(&m);
}

JNIEXPORT void JNICALL
Java_com_rsjian2_cs296_ServerActivity_notifyMovePlayed(
	JNIEnv* env,
	jobject obj,
	jint idx)
{
	notifyMovePlayed(idx);
}

JNIEXPORT void JNICALL
Java_com_rsjian2_cs296_ClientActivity_notifyMovePlayed(
	JNIEnv* env,
	jobject obj,
	jint idx)
{
	notifyMovePlayed(idx);
}
