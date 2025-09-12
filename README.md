[![Download](https://img.shields.io/github/downloads/areteruhiro/LIMEs/total
)]
# <img src="app/src/main/ic_launcher-playstore.png" width="60px"> LIMEs: Adkiller for LINE

GitHubは更新を頻繁にしません、テレグラム/Discordで最新の情報を確認してください
# 注意
 本モジュールは学習目的でのみ開発・利用するものです。
Android のアプリケーション動作原理、フック処理、リバースエンジニアリングに関する技術理解を深めることを目的としており、
LINE 株式会社やその他第三者のサービス提供を妨害したり、営利目的で利用することは意図していません。
実際の利用においては、各サービスの利用規約や関連法令を遵守してください。
本モジュールを利用した結果について、開発者は一切の責任を負いません。

# Support Server

https://discord.gg/5WWxUkMsNJ
 # 導入方法
 
 [ROOT](https://giteruthub.com/arehiro/LIMEs/blob/master/README%20for%20root.md) 
 
 [LsPatch](https://github.com/areteruhiro/LIMEs/blob/master/README%20for%20LsPatch.md) 


## 概要

"LINEをより使いやすく"

[機能リスト](https://github.com/areteruhiro/LIMEs/blob/master/FunctionLIST.md) 
 

# 確認済みのバグやエラー

## トーク画像のリストアについて

①chats_backupフォルダを長押しして別のフォルダに移動<br>
②LINEを開いてトーク画像フォルダのバックアップを開始をクリック<br>
④別の場所にあるフォルダを　LIME backup フォルダに移動させ入れ替え<br>

⑤トーク画像のリストアボタンを押す　

ファイルエクスプローラーは以下を使用してください（エラーなどの報告に対応しやすくするためです）
https://play.google.com/store/apps/details?id=me.zhanghai.android.files


方法が怪しい場合以下の動画を参照してから、リストアを行ってください。(データが上書きされリストアできなくなる恐れがあります)
https://youtu.be/94JN4NLGdOI


## トーク履歴の自動バックアップについて

[Macro SAMPLE](https://drive.usercontent.google.com/u/0/uc?id=1rhZPmoMbti_l1JaX2EbjcRKUePkWlIXU&export=download)

または以下を参考にしてください
https://github.com/areteruhiro/LIMEs/issues/10



# 寄付

以下の方々のおかげで開発を継続できています。大変感謝しています。<br>
We are very grateful to the following people for making this possible:

@Kansakitwさん
@ハチワレさん 
@WE ZARDさん
@匿名希望さん
@ユウさん
@Yukiさん
@Fuku5656 さん
@としさん
@kurage pucapucaさん
@Naotoさん
@C1el_55K
@かなでさん
@Xさん
@さくらさん
@nさん
@まつそうさん
@sさん
@Yobuさん
@まさしさん
@Yukiさん
@けーすけさん
@けけさん


継続した開発時間の確保のため寄付のほどお願いいたします<br>
Please donate to ensure continued development time.

* [Pay Pay](https://qr.paypay.ne.jp/p2p01_ZcPhJJ3YF3cS6sKv)<br>

*PayPay ID
hiro_1114
* [Amazon Gift Card](https://www.amazon.co.jp/gp/product/B004N3APGO) Send to (limebeta.dev@gmail.com)<br>

* [PayPal](Contact us / お問い合わせください)
https://t.me/areteruhiro

# 更新について

1. メジャーバージョン (X)：<br>
最も左の数字で、システム全体の大規模な変更や重要な機能追加、または互換性のない変更があった際に上げます。﻿<br>
2. マイナーバージョン (Y)：<br>
中央の数字で、既存の機能に後方互換性のある機能追加があった際に上げます。﻿<br>
3. パッチバージョン (Z)：<br>
最も右の数字で、後方互換性のあるバグ修正や軽微な修正があった際に上げます<br>

https://egg.5ch.net/test/read.cgi/android/1740022664/667　より引用

## Thank you

LIME  開発者
https://github.com/Chipppppppppp

コラボレーター
https://github.com/s1204IT


apks→apk

①AntiSplit<br>
https://github.com/AbdurazaaqMohammed/AntiSplit-M


②M apk tool<br>
https://maximoff.su/apktool/?lang=en

Icon
https://github.com/reindex-ot

バグ報告、仕様提案
5チャンネラー
https://egg.5ch.net/test/read.cgi/android/1729438846/


## 使用方法


LINEアプリの <kbd>ホーム</kbd> > <kbd>⚙</kbd> から｢**設定**｣に入り、右上の｢**LIME**｣のボタンより開けます。また、Root ユーザーは LI**M**E アプリから設定することも可能です。クローンアプリなどでは LI**M**E 側からしか設定できない場合があるようです。

<details><summary>画像を閲覧</summary>

<a href="#"><img src="https://github.com/Chipppppppppp/LIME/assets/78024852/2f344ce7-1329-4564-b500-1dd79e586ea9" width="400px" alt="Sample screenshot"></a>

</details>

また、トーク画面上の <kbd>トグル又は✉️ボタン</kbd> かオン（緑または、未開封）にすると**未読のまま閲覧**ができます。(このスイッチは設定で削除可能です）

※返信すると未読が解除されてしまうのでご注意ください

<details><summary>画像を閲覧</summary>
<img src="https://github.com/user-attachments/assets/a9ee3b95-f785-4fac-9937-b904fe84f7b2" width="400px" alt="Sample screenshot">
</details>


### 1. デバイス、アプリバージョンを偽装してログイン
この機能は自己責任です

### 3. Android ID を偽装する
この方法は**両方のデバイスを Root 化している**場合のみ可能です。  
<https://jesuscorona.hatenablog.com/entry/2019/02/10/010920> にあるように、メッセージの同期などに若干の遅れが生じることに注意が必要です。

<details>

- メリット：3 端末以上でもログイン可能・すべてのサービスを使用可能
- デメリット：メッセージの同期に遅れが生じる・Root 限定

#### 手順

1. LINE と LIME をインストールする
2. LINE ログイン画面で、「複数デバイスログイン (Android ID を偽装)」にチェックを入れる
3. <kbd>設定</kbd> > <kbd>アプリ</kbd> > <kbd>LINE</kbd> より、LINE アプリの設定画面から「強制停止」と「ストレージとキャッシュ」の「キャッシュを削除」をタップ
4. LINE アプリを再度開き、ログインする
5. ログイン後、[Swift Backup](https://play.google.com/store/apps/details?id=org.swiftapps.swiftbackup) を利用して LINE アプリをバックアップ (詳しくは[こちら](https://blog.hogehoge.com/2022/01/android-swift-backup.html))
6. Swift Backup のバックアップフォルダをもう一つの端末に移し、バックアップした LINE をインストール (詳しくは[こちら](https://blog.hogehoge.com/2022/05/SwiftBackup2.html))
7. LINE アプリを**開かず**に先に LIME をインストールする

</details>

## 問題の報告

新たなバグや修正方法を見つけた場合は、報告 をお願いします。

To foreigners, please translate your report into English and submit it rather than translating it into Japanese.

