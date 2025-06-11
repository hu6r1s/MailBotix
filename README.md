# 📬 MailBotix (Spring Boot 기반 Mail 자동 답변 생성기)

**MailBotix**는 사용자의 Mail 메일 목록을 조회하고, 특정 메일을 선택하면 Google Gemini AI를 통해 해당 메일에 대한 자연스러운 답변을 자동으로 생성해주는 서비스입니다.  
Spring Boot 기반으로 개발되었으며, Mail API(Google)와 Gemini를 연동하여 메일 관리와 자동화된 응답을 지원합니다.

---

## ✅ 주요 기능

- 🔐 OAuth 인증
- 📥 받은 메일 목록 조회
- 📄 특정 메일 내용 조회
- 🤖 Gemini AI를 통한 자동 답변 생성
- 📤 메일 답변 전송

---

## ⚙️ 기술 스택

- Java 17
- Spring Boot 3.4.4
- Gmail API (Google Cloud)
- Gemini (Google AI)
- Naver API
- Redis
- SpringDoc (Swagger)
- IMAP, SMTP
- ELK, Filebeat
---

## 🚀 성능 개선
MailBotix는 사용자 경험을 개선하기 위해 주요 기능들의 응답 속도를 최적화했습니다. 특히, 지연 시간이 길었던 IMAP/SMTP 기반 메일 목록 조회 및 전송 기능에서 눈에 띄는 성능 향상을 달성했습니다.
### 1. 메일 목록 조회 성능 개선
- 문제 현상
  - Naver 메일 (100개): 초기 응답 시간 36.81초
  - Google 메일 (100개): 초기 응답 시간 약 8.5초
- 원인 분석
  - 초기 구현에서는 메일 목록을 가져온 후, 각 메일의 정보(보낸사람, 제목, 읽음 상태 등)를 얻기 위해 루프 내에서 개별적으로 요청을 전송했습니다. 이는 메일 개수(N)만큼 추가적인 네트워크 요청이 발생하는 N+1 문제를 유발하여, 조회할 메일이 많아질수록 응답 시간이 기하급수적으로 늘어나는 원인이었습니다.
- 개선 과정
  - 구글
  - 1. Google API Client Library의 BatchRequest 기능을 도입하여 여러 개의 API 요청을 하나로 묶어 처리하도록 로직을 변경
  - 네이버
  - 1. Folder.fetch() 도입: JavaMail API의 fetch() 메소드를 사용하여, 필요한 모든 정보(Envelope, Flags, UID 등)를 단 한 번의 네트워크 요청으로 미리 가져오도록 로직을 변경
  - 2. 병렬 처리 (parallelStream): fetch로 가져온 데이터를 Java의 parallelStream()을 사용하여 병렬로 처리함으로써, 많은 양의 메일을 처리할 때의 전체 소요 시간을 추가로 단축
- 결과
  - Naver 메일 (100개): 36.81초 → 2.20초 (약 94% 성능 향상)
  - Google 메일 (100개): 8.5초 → 950ms (약 88% 성능 향상)

### 2. 메일 발송 성능 개선 (SMTP 비동기 처리)
- 문제 현상
  - Naver 메일 발송: 평균 응답 시간 5.09초
- 원인 분석
  - SMTP transport.connect() 및 transport.sendMessage()는 동기적으로 동작하여, 메일 발송이 완료될 때까지 HTTP 요청 스레드를 차단했습니다. 이로 인해 사용자는 메일이 완전히 전송될 때까지 기다려야 했습니다.
- 개선 과정
  - Spring @Async 적용: 메일 발송 로직을 별도의 서비스 메소드로 분리하고 @Async 어노테이션을 적용했습니다. 이를 통해 컨트롤러는 메일 발송 요청을 받자마자 즉시 사용자에게 응답을 보내고, 실제 SMTP 통신은 백그라운드 스레드에서 비동기적으로 처리하도록 변경
- 결과
  - Naver 메일 발송: 5.09초 → 92ms (약 98% 성능 향상)

## 🔧 트러블슈팅
개발 과정에서 마주친 주요 기술적 문제들과 해결 과정을 공유합니다.
1. @Async 비동기 메소드에서 ThreadLocal 데이터 소실
- 문제 현상
  - @Async가 적용된 메일 발송 메서드에서 AppPasswordContext.get() 호출 시, ThreadLocal에 저장된 비밀번호 정보를 가져오지 못함
- 원인 분석
  - @Async는 별도의 스레드 풀에서 동작하므로, 기존 웹 요청 스레드에 저장된 ThreadLocal 값이 비동기 스레드에 전달되지 않음
- 해결 방안 
  - 별도의 MailAsyncService 클래스를 만들고, @Async가 적용된 메서드를 이곳에 정의
  - 메인 서비스에서는 MailAsyncService를 주입받아 호출함으로써, ThreadLocal에 의존하지 않는 구조로 변경
2. 네이버 메일 API 비제공
- 문제 현상
  - 네이버는 공식적인 메일 API를 제공하지 않음
- 해결 방안
  - IMAP/SMTP 프로토콜을 사용하여 메일 조회 및 전송 기능을 직접 구현
  - 여기서 네이버 IMAP/SMTP는 OAuth 2.0을 지원하지 않아, 프론트엔드에서 직접 비밀번호를 받아 사용
    - 보안을 위해 사용자의 비밀번호는 AES로 암호화하여 쿠키에 저장하고, 서버에서는 복호화 후 인증에 사용
