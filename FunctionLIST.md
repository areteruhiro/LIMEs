
## 機能
### 一般
- 設定をLINEに埋め込まない
VOOM アイコンを削除
ウォレットアイコンを削除
ニュースアイコンを削除
ボトムバーのアイコンを均等に配置
ボトムバーのアイコンの当たり判定を拡張する
ボトムバーのアイコンのラベルを削除
サービスの項目を削除
サービスのラベルを削除
更新されたプロフィールを削除
チャットリスト内でのオープンチャットアイコンを削除する
チャットリスト内でのアルバムアイコンを削除する
チャットリスト内のAIボタンの削除
検索バーを削除する
WebView を既定のブラウザにリダイレクト
ブラウザアプリで開く
RemoveCreateChatButton 15.12.2~
removeMoreButton 15.12.2~
RemoveCommerce
### 通知
通知から"通知をオフアクションを削除
登録しているグループ名の通知をオフにする
LINE通知のカスタマイズ
GroupNotification
特定のユーザー名、グループ名のみ以外の通知を無効化
通知にコピーアクションを追加する
オリジナル通知IDを使う(返信バグは修正されますが通知 が上書きされます)
DevNow サイレントメッセージの無効(次回の通知ありの 通知で通知されます)
NotificationReaction
### チャット
常に既読をつけない
送信取り消しを拒否
常にミュートメッセージとして送信
非表示にしたチャットの再表示を無効化
送信したメッセージの既読者の確認
(既読機能) 自分以外のメッセージを表示しない
(緊急用) 既読確認ボタンの右にデータ削除ボタンを作成す る
「未読のまま閲覧」 スイッチを削除
LsPatch用 「未読のまま閲覧」 スイッチを表示
音声ボタンを無効化
ブロック監視[誤検知可能性有]
通話項目LINEプリを削除
通話項目 音声通話項目を非表示に
通話項目 グループ内でのビデオ通話項目を削除
通話項目 個人チャット内でのビデオ通話項目を削除
チャットリストのピン設定
MediaReNameSave (DevNow)
ReactionCount
SendEnterChange_ChatList
SendEnterChange_InChat
MessageSend
Al Summary Delete Button
ReactionList
閲覧すると通知数が消える
Keep_unread_PopupListView
PreventMarkAsRead_Setting
Disable_chat_ui_ai_talk_suggestion
### 広告
広告を削除
おすすめを削除
LYP プレミアムのおすすめを削除
RemoveGcsLypRecommend
### 着信音/発信音
本体の発着信音を鳴らす(LsPach用)
(発信音を鳴らす)着信音を利用する
(Root用) 着信音のミュート
発信音を無効にする
SilentCheck
通話画面にLINEを開くボタンを作成
MakeStopCallToneButton
CallComing
CallDisconnectTone
Original_Tone(15.12.2~)
### テーマ
ナビゲーションのボタンの色を黒にする、ダークテーマ をピュアダークにする
ピュアダークモードを端末のダークモードと同期させる
WhiteToDark(DevNow)
Enable_Theme_Validation
(Wallete)RemoveLayout
### その他
LINE バージョンの確認を停止
通信内容をログに出力 (開発者用)
年齢確認をスキップ
(AntiApk) 設定項目トークボタンを開く
更新を自動的に確認する
新しい設定画面にする
downgrade_allow
SystemForegroundService

- 不要なボトムバーのアイコンの削除
- ボトムバーのアイコンのラベルの削除
- 広告・おすすめの削除
- サービスのラベルを削除
- 通知の「通知をオフ」アクションを削除
- WebView を既定のブラウザで開く
  <br>
- 送信取り消しの拒否
- 常にミュートメッセージとして送信
  - 送信時「通常メッセージ」を選択すれば通知されます
- トラッキング通信のブロック(この機能は自己責任です)
  - `noop`, `pushRecvReports`, `reportDeviceState`, `reportLocation`, `reportNetworkStatus` がブロックされます
- 通信内容をログに出力
- 通信内容を改変
 [- JavaScript で通信内容を改変できます](https://github.com/areteruhiro/LIMEs/blob/master/JavaRead.md)


  <br>以下から追加機能
- ナビゲーションバーを黒色に固定化、ブラックテーマをナチュラルブラックに変更
- 非表示にしたチャットの再表示を無効化
- LsPatch用　着信音を鳴らす - 着信音のミュート化- 発信音のミュート化
- サービスの項目の削除
    <br>
- 未読のまま閲覧ボタンの位置調整
- 常に既読をつけないの仕様変更
  <br>
- トークのバックアップ、リストア（外部からのIntentでバックアップが可能）
- 既読者の確認
- 音声ボタンの無効化
- 通知に画像、スタンプを添付するようにする、通知を更新せずに新しい通知として作成する
- 通知にテキストをコピーするアクションの追加
- 登録しているグループ名の通知を無効化
- 更新されたプロフィールの削除
- 複数バージョンに対応 <br>
- 特定のグループ、ユーザー以外の通知を無効化 <br>
- LIMEsの設定をファイル上で行うように <br>
- 取り消されたメッセージをわかりやすくする機能の追加 <bg>
- 通話画面にLINEを開くボタンの追加 
- 通話項目からプリ項目を削除
- リアクションカウンター機能
- Media Rename Save 写真、動画保存時のファイル名を変更
- サイレントメッセージの通知を発行　(DisableSilentMessage)
- Reaction Count LINE本来のリアクションリストにカウント機能を追加します
- リアクションされた際に通知を発行　(NotificationReaction)
- エンターキーで送信をチャット内　OR チャットリストで変更 (SendEnterChange_ChatList SendEnterChange_InChat)
- ホームのRecommend欄の削除　RemoveGcsLypRecommend
- リアクション一覧ポップアップ表示ボタンの作成　ReactionList_db
- 切断時に切断音を鳴らすCallDisconnectTone
- 
