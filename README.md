# DTNFileShare
村越允

DTNFileShareのプロジェクト

SelectDestinationActivity.javaのSLACK_BOT_TOKENは認証トークンを公開するとslackがそれを無効化してしまうため""にしている

変更 
20210629
Slackに受信したことを通知するためのSendSlackActivityを追加.
ファイルを送った人のEID,ファイル名は取得できた.
あとSelectDestinationActivityにおけるEIDDao型のdao,自分のEID,slackのidを取得するコードを追加する予定
SendSlackActivityによって送信時にSlackに通知が送られなくなってしまったため直さないといけない.
SelectDestinationActivityで送信時にSlackに通知を送っているためSendSlackActivityにまとめる予定.
