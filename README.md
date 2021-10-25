# DTNFileShare
村越允

DTNFileShareのプロジェクト

SendSlackMessaage.javaのSLACK_BOT_TOKENは認証トークンを公開するとslackがそれを無効化してしまうため""にしている

Android Studio バージョン4.1.3

DTNFileShare全体の使い方の説明
[https://github.com/kennkyuu932/DTNFileShare_explanation](https://github.com/kennkyuu932/DTNFileShare_explanation)

動作端末

Xperia 1 (Androidバージョン 10,11)

Galaxy A41 (Androidバージョン 11)

VAIO Phone A (Androidバージョン 6.0.1)

## 使い方

/app/scr/main/java/de/tub/ibr/dtn/sharebox/SendSlackMessage.javaの変数SLACK_APP_TOKENに作成したSlack appのBot User OAuth Token の文字列を入力

/app/scr/main/java/de/tub/ibr/dtn/sharebox/SendSlackMessage.javaの変数SLACK_APP_URLにglitch等で作成したサーバーのURLを入力

/app/scr/main/java/de/tub/ibr/dtn/sharebox/RegisterEIDActivity.javaの変数SLACK_APP_URLにglitch等で作成したサーバーのURLを入力

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

#### 20211023
通知を行うファイル共有の通信性能評価を行うためにSlackのWeb APIを呼び出したときにその情報を外部ファイルに保存できるようにした

READMEにこのアプリの使い方を記述