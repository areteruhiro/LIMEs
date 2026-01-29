## 方法①導入済みのものを利用する
[Discordサーバー](https://discord.gg/5WWxUkMsNJ)の limes-release チャンネルからLIMEsとLINEをダウンロードしてインストールしてください。<br>
> [!TIP]
> 配布のローカル適応版を使う場合は、方法②の手順でLSPatchのインストールも必要です。<br>
LINEをローカル適応でインストールすることにより、LINEを毎回触らずに、LIMEsのみを動的に更新できます。<br>

⚠LIMEs自体のインストールも必ず必要です

## 方法②自分で導入する

1. [**JingMatrix LSPatch**](https://github.com/JingMatrix/LSPatch/) をインストール

2.パッチするapkについて
### LINE
[アーキテクチャの確認](https://play.google.com/store/apps/details?id=com.ytheekshana.deviceinfo)→CPU→サーポートされているABI<br>
※基本最近に発売されている端末なら、arm64-v8aが多いです
#### ダウンロード先
[APKMirror](https://www.apkmirror.com/apk/line-corporation/line/)<br>
[APKPure](https://apkpure.com/jp/line-calls-messages/jp.naver.line.android)  ※一部バージョンが欠けています<br>
LINE公式  https://line-android-universal-download.line-scdn.net/line-15.12.2.apk <br>
※URLのバージョンの部分を変えると他のバージョンもダウンロードできます。<br>

M apk tool <br>
https://maximoff.su/apktool/?lang=en

でapkに変換してからパッチしてください 

apkへの変え方
![AntiSplit.jpg](https://github.com/user-attachments/assets/a0a7b6c4-ff50-4e79-96dd-0ec71de25ddb)

2. **LSPatch** アプリを開き、<kbd>管理</kbd> > 右下の <kbd>＋</kbd> > <kbd>ストレージからapkを選択</kbd> >  先程ダウンロードした LI**N**E の APK を選択 > <kbd>統合</kbd> → <kbd>モジュールを埋め込む</kbd> > <kbd>**インストールされているアプリを選択**</kbd> > LI**M**Es にチェックを入れて <kbd>＋</kbd> > <kbd>パッチを開始</kbd> より、パッチを適用

※[この方法](https://github.com/Chipppppppppp/LIME/issues/50#issuecomment-2174842592) を用いればトークの復元が可能なようです。

> [!TIP]
> <kbd>ディレクトリの選択</kbd>と出てきた場合は、<kbd>OK</kbd> を押してファイルピッカーを起動し、任意のディレクトリ下にフォルダを作成し、<kbd>このフォルダを使用</kbd> > <kbd>許可</kbd>を押す

3. [**Shizuku**](https://github.com/RikkaApps/Shizuku) を使用している場合は <kbd>インストール</kbd> を押して続行する  
  使用していない場合は、ファイルエクスプローラー等の別のアプリからインストールする


## 通知が届かない場合
(こちらの手順)[https://github.com/areteruhiro/LIMEs/blob/master/LSPatch-Notify.md]にまとめています。 <br>
まずはこちらの手順に沿ってお試しください
