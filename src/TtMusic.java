import com.alibaba.fastjson.JSON;
import tt.TiantianDatas;
import tt.TiantianLrc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qtfreet on 2017/2/6.
 */
public class TtMusic implements IMusic {


    private static List<SongResult> search(String key, int page, int size) throws Exception {
        String url = "http://search.dongting.com/song/search?page=" + page + "&user_id=0&tid=0&app=ttpod&size=" + size + "&q=" + key + "&active=0";
        String s = NetUtil.GetHtmlContent(url);
        System.out.println(s);
        TiantianDatas ttDatas = JSON.parseObject(s, TiantianDatas.class);
        if (ttDatas == null) {
            return null;//搜索歌曲失败
        }
        if (ttDatas.getTotalCount() == 0) {
            return null;//没有搜到歌曲
        }
        int totalsize = ttDatas.getTotalCount();
        List<TiantianDatas.DataBean> data = ttDatas.getData();

        return GetListByJson(data);
    }

    //解析搜索时获取到的json，然后拼接成固定格式
    private static List<SongResult> GetListByJson(List<TiantianDatas.DataBean> songs) throws Exception {
        List<SongResult> list = new ArrayList<>();
        System.out.println(JSON.toJSONString(songs));
        int len = songs.size();
        if (len <= 0) {
            return null;
        }
        for (TiantianDatas.DataBean song : songs) {
            SongResult songResult = new SongResult();
            NetUtil.init(songResult);
            TiantianDatas.DataBean songsBean = song;
            List<TiantianDatas.DataBean.UrlListBean> links = songsBean.getUrlList();
            if (JSON.toJSONString(links).equals("[]") || links == null || links.size() == 0) {
                continue;
            }
            String SongId = String.valueOf(songsBean.getSongId());
            String SongName = songsBean.getName();
            String Songlink = "http://h.dongting.com/yule/app/music_player_page.html?id=" + String.valueOf(songsBean.getSongId());
            String ArtistId = String.valueOf(songsBean.getSingerId());
            String AlbumId = String.valueOf(songsBean.getAlbumId());
            String AlbumName = songsBean.getAlbumName();
            String artistName = songsBean.getSingerName();
            songResult.setArtistName(artistName);
            songResult.setArtistId(ArtistId);
            songResult.setSongId(SongId);
            songResult.setSongName(SongName);
            songResult.setSongLink(Songlink);
            songResult.setAlbumId(AlbumId);
            songResult.setAlbumName(AlbumName);
            int mvs = songsBean.getMvList().size();
            if (songsBean.getMvList() != null && mvs > 0) {
                int max = 0;
                for (int j = 0; j < mvs; j++) {
                    TiantianDatas.DataBean.MvListBean mvListBean = songsBean.getMvList().get(j);
                    int videoId = mvListBean.getVideoId();
                    songResult.setMvId(String.valueOf(videoId));
                    if (max == 0) {
                        songResult.setMvHdUrl(mvListBean.getUrl());
                        songResult.setMvLdUrl(mvListBean.getUrl());
                        max = mvListBean.getBitRate();
                    } else {
                        if (mvListBean.getBitRate() > max) {
                            songResult.setMvHdUrl(mvListBean.getUrl());
                        } else {
                            songResult.setMvLdUrl(mvListBean.getUrl());
                        }
                    }
                }
            }
            int urlsize = links.size();

            for (TiantianDatas.DataBean.UrlListBean link : links) {
                songResult.setLength(Util.secTotime(link.getDuration() / 1000));
                switch (link.getBitRate()) {
                    case 128:
                        songResult.setLqUrl(link.getUrl());
                        songResult.setBitRate("128K");
                        break;
                    case 192:
                        songResult.setHqUrl(link.getUrl());
                        songResult.setBitRate("192K");
                        break;
                    case 320:
                        if (songResult.getHqUrl().isEmpty()) {
                            songResult.setHqUrl(link.getUrl());
                        }
                        songResult.setSqUrl(link.getUrl());
                        songResult.setBitRate("320K");
                        break;
                }
            }
            if (songsBean.getLlList() != null && songsBean.getLlList().size() != 0) {
                songResult.setBitRate("无损");
                int size = songsBean.getLlList().size();
                for (int k = 0; k < size; k++) {
                    TiantianDatas.DataBean.LlListBean llListBean = songsBean.getLlList().get(k);
                    switch (llListBean.getSuffix()) {
                        case "flac":
                            songResult.setFlacUrl(llListBean.getUrl());
                            break;
                        case "ape":
//                            songResult.setFlacUrl(llListBean.getUrl());
                            break;
                        case "wav":
//                            songResult.setFlacUrl(llListBean.getUrl());
                            break;
                        default:
                            songResult.setFlacUrl(llListBean.getUrl());
                            break;

                    }
                }
            }
            songResult.setType("tt");
            songResult.setPicUrl(songsBean.getPicUrl());
//            songResult.setLrcUrl(GetLrcUrl(SongId, SongName, artistName)); //暂不去拿歌曲，直接解析浪费性能
            list.add(songResult);
        }
        return list;
    }

    private static String GetLrcUrl(String songId, String songName, String ArtistName) {
        try {
            //   SearchSong(songId);
            String s = NetUtil.GetHtmlContent("http://lp.music.ttpod.com/lrc/down?artist=" + UrlEncode(ArtistName) +
                    "&title=" + UrlEncode(songName) + "&song_id=" + songId);
            System.out.println(s);
            TiantianLrc kugouLrc = JSON.parseObject(s, TiantianLrc.class);

            return kugouLrc.getData().getLrc();
        } catch (Exception e) {
            return "";
        }
    }

    private static String UrlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    @Override
    public List<SongResult> SongSearch(String key, int page, int size) {
        try {
            return search(key, page, size);
        } catch (Exception e) {
            return null;
        }
    }
}
