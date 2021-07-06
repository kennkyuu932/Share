# DTNFileShare
村越允

DTNFileShareのプロジェクト

SelectDestinationActivity.javaのSLACK_BOT_TOKENは認証トークンを公開するとslackがそれを無効化してしまうため""にしている

## 変更 

#### 20210629

Slackに受信したことを通知するためのSendSlackActivityを追加.
ファイルを送った人のEID,ファイル名は取得できた.
あとSelectDestinationActivityにおけるEIDDao型のdao,自分のEID,slackのidを取得するコードを追加する予定
SendSlackMessageによって送信時にSlackに通知が送られなくなってしまったため直さないといけない.
SelectDestinationActivityで送信時にSlackに通知を送っているためSendSlackMessageにまとめる予定.

#### 20210630

現在は自分のEIDを取得するメソッドにVAIOのEIDを書いている
ファイル受け取り時に自分のEIDを取得できればダウンロード通知はできる
SendSlackMessageでSlackAPIを用いてメッセージを送る工程をConversationOpenというメソッドとした
メッセージ送信を一つのメソッドにまとめるためコンストラクタを作った
ファイル送信時に自分のEIDを取ってこれるようにした
(DtnService.java 166行目のgetClientEndPointメソッドを用いた)

#### 20210701
ファイルの送受信時にSlackにメッセージを送信する工程を一つのクラスでまとめた

#### 20210705
細かい修正

#### 20210706
アプリ起動時の画面からアプリに登録されているユーザーリストを表示することができるようにした
(メニュータブからSelectDestinationActivityで表示される画面を呼び出す)
