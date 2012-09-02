日本語音声認識エンジン Julius for Android
=========================================

日本語大語彙連続音声認識エンジン [Julius](http://julius.sourceforge.jp) をAndroid上で動作させ、オフライン音声認識を行うサンプルアプリケーションです。
連続音声認識と記述文法の双方を扱うことができます。

環境
----

以下の環境で動作確認しています。

* OS:Mac OS X Lion (10.7.4)
* Android: Nexus 7 (Android 4.1.1)
* Android SDK:API-15
* Android NDK:r8b

インストール
------------

1. リポジトリの "sdcard" ディレクトリ内にある "julius" 以下をそのまま、Androidデバイスの外部ストレージ(sdcard等)にコピーしてください
1. julius-4.2.2ディレクトリでandroid_build.shを起動してください。
1. jniディレクトリでndk-buildしてください。
1. APKを作成し、Androidへデプロイしてください。

注意
----

* 付属のクロスビルド用のシェル（julius-4.2.2/android_build.sh）は、各環境に応じて適時修正してください。ただしIntel系列のなどエンディアンが異なるCPUの場合、このままでは動作しない可能性があります。
* 付属の記述文法は「(蜜柑|りんご|ぶどう)が[0-9]個」という文法のみ受け付けます。設定ファイルや記述文法の定義ファイルは、[Julius](http://julius.sourceforge.jp)のマニュアル[The Juliusbook](http://julius.sourceforge.jp/index.php?q=documents.html#juliusbook)を参考に修正してください。
* 語彙辞書や記述文法はShift-JISでエンコードされている前提です。
* Juliusにはマイクからの直接入力を扱う機能がありますが、このサンプルでは利用できません。入力した音声を一旦「モノラル 16bit 22050Hz」のPCM音源としてファイルに保存し、Juliusに認識させています。

Juliusの改変
------------

Android上で動作させるために、下記のJuliusのソースコードを一部改変しています。

* julius-4.2.2/libsent/src/ngram/ngram_read_bin.c
* julius-4.2.2/libsent/src/hmminfo/read_binhmm.c

双方とも、ANDROID_CUSTOMがdefineされている場合、一部のエンディアン入れ替え処理をスキップします。  
改変日は2012/09/01、改変者は nobuyuki.matsui@gmail.com です。

License
-------

Juliusのライセンスは、下記の著作者が著作権を留保しています。ライセンスの詳細は julius-4.2.2/LICENSE.txt を参照してください。

> Copyright (c)   1991-2012 京都大学 河原研究室  
> Copyright (c)   1997-2000 情報処理振興事業協会(IPA)  
> Copyright (c)   2000-2005 奈良先端科学技術大学院大学 鹿野研究室  
> Copyright (c)   2005-2012 名古屋工業大学 Julius開発チーム  


ビルドスクリプトやJavaソース等、Julius以外の部分のライセンスは下記GPLV3に従います。  
Copyright(C) 2012 Nobuyuki Matsui (nobuyuki.matsui@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
