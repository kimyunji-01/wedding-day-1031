# 결혼식 사진첩

## 사진·영상 넣는 법
1. `media/spot1/`, `media/spot2/`, `media/spot3/` 폴더에 해당 스팟 사진·영상을 넣기 (원본 그대로 OK)
   - 사진은 jpg/png, 영상은 mp4 권장 (아이폰 HEIC 는 jpg 로 변환해서)
2. 프로젝트 루트에서 `java Build.java` 실행 (JDK 11 이상) → `thumbs/` 와 `items.json` 자동 생성
3. GitHub에 push → Pages 반영까지 몇 분

## 미리보기
`python -m http.server 8000` 또는 VS Code/Cursor 의 Live Server 로 열기 (index.html 더블클릭은 안 돼요)

## 편지
`letter.txt` 에 쓰면 줄바꿈 그대로 편지지에 나와요. 파일을 비우거나 지우면 편지 버튼이 사라져요.

## 스팟 이름·위치 바꾸기
`spots.json` 의 `name`, `x`, `y`(지도 이미지 기준 %) 를 수정. (`items.json` 은 자동 생성이니 손대지 않기)
스팟 추가: 항목을 복사해 `id` 를 새로 짓고 `media/<id>/` 폴더 만들기.

## 영상
- 아이폰 .mov 변환: `ffmpeg -i in.mov -vcodec libx264 -crf 26 -vf scale=-2:1080 out.mp4`
- GitHub 웹 업로드는 파일당 25MB 제한 → 큰 파일은 `git push` 로.

## 전체 다운로드
기본은 브라우저에서 zip 으로 묶어줘요(아주 크면 느릴 수 있음).
미리 만든 zip 이 있으면 `spots.json` 스팟에 `"zip": "media/spot1.zip"` 처럼 적으면 그 파일로 연결돼요.

## 공개 주의
무료 Pages 는 공개 레포라 주소를 아는 사람은 누구나 볼 수 있어요. 레포 이름은 추측하기 어렵게, 링크는 필요한 사람에게만.
