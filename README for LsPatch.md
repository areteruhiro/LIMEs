## 方法①導入済みのものを利用する

1,Patched LINE <br>
2,LIMEs モジュールをインストール

![Screenshot_2025-05-26-22-03-29-905_com.github.android-edit.jpg](https://github.com/user-attachments/assets/838ca346-8423-4224-813e-80760643bf35)


⚠LIMEs自体のインストールも必ず必要です

## 方法②自分で導入する

1. [**JingMatrix LSPatch**](https://github.com/JingMatrix/LSPatch/) をインストール

1.5パッチするapkについて
LINE  <br>

https://www.apkmirror.com/apk/line-corporation/line/

クラッシュやなんらかのエラーが発生した場合、自分の端末のアーキテクチャに対応するファイルをダウンロード

[アーキテクチャの確認](https://play.google.com/store/apps/details?id=com.ytheekshana.deviceinfo)→CPU→サーポートされているABI


・armeabi-v7a
https://line-android-universal-download.line-scdn.net

・arm64-v8a https://d.apkpure.com/b/XAPK/jp.naver.line.android?


M apk tool <br>
https://maximoff.su/apktool/?lang=en
でapkに変換してからパッチしてください 

apkへの変え方
![AntiSplit.jpg](https://github.com/user-attachments/assets/a0a7b6c4-ff50-4e79-96dd-0ec71de25ddb)

2. **LSPatch** アプリを開き、<kbd>管理</kbd> > 右下の <kbd>＋</kbd> > <kbd>ストレージからapkを選択</kbd> >  先程ダウンロードした LI**N**E の APK を選択 > <kbd>統合</kbd> → <kbd>モジュールを埋め込む</kbd> > <kbd>**インストールされているアプリを選択**</kbd> > LI**M**E にチェックを入れて <kbd>＋</kbd> > <kbd>パッチを開始</kbd> より、パッチを適用

※[この方法](https://github.com/Chipppppppppp/LIME/issues/50#issuecomment-2174842592) を用いればトークの復元が可能なようです。

> [!TIP]
> <kbd>ディレクトリの選択</kbd>と出てきた場合は、<kbd>OK</kbd> を押してファイルピッカーを起動し、任意のディレクトリ下にフォルダを作成し、<kbd>このフォルダを使用</kbd> > <kbd>許可</kbd>を押す

3. [**Shizuku**](https://github.com/RikkaApps/Shizuku) を使用している場合は <kbd>インストール</kbd> を押して続行する  
  使用していない場合は、ファイルエクスプローラー等の別のアプリからインストールする


-通知が届かない場合
<14.4.2からパッチインストールを行う>


## トーク履歴の自動バックアップについて

[Macro SAMPLE](https://drive.usercontent.google.com/u/0/uc?id=1rhZPmoMbti_l1JaX2EbjcRKUePkWlIXU&export=download)

または以下を参考にしてください
https://github.com/areteruhiro/LIMEs/issues/10



