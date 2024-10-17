import java.util.HashMap;

public class demo {
    public static void main(String[] args) {
        String simpleURL="https://www.liepin.com/zhaopin/?currentPage=0&pageSize=40&city=410&dq=410&pubTime=&key=%E5%89%8D%E7%AB%AF&suggestTag=&otherCity=&industry=&sfrom=search_job_pc&ckId=deu5zqbpdqopsypb2sfftp9xm31arxf8&scene=input&skId=deu5zqbpdqopsypb2sfftp9xm31arxf8&fkId=deu5zqbpdqopsypb2sfftp9xm31arxf8&suggestId=";
        String       URL="https://www.liepin.com/zhaopin/?city=410&dq=410&pubTime=&currentPage=0&pageSize=40&key=%E5%89%8D%E7%AB%AF&suggestTag=&workYearCode=&compId=&compName=&compTag=&industry=&salary=&jobKind=&compScale=&compKind=040&compStage=&eduLevel=&comp=&otherCity=&ckId=5q33ab4279qidj6aghku05j2pcdmlqo9&scene=condition&skId=6mq5xk4lq1p1fknsaco4ko09upu8uabb&fkId=5q33ab4279qidj6aghku05j2pcdmlqo9&sfrom=search_job_pc&suggestId=";

        simpleURL = simpleURL.replace("https://www.liepin.com/zhaopin/?","");
        URL = URL.replace("https://www.liepin.com/zhaopin/?","");

        String[] simpleSplit = simpleURL.split("&");
        String[] split = URL.split("&");

        HashMap<String, String> simpleMap = new HashMap<>();
        for (String value : simpleSplit) {
            String[] result = value.split("=");
            if(result.length>1){
                simpleMap.put(result[0],result[1]);
            }else {
                simpleMap.put(result[0],"");
            }
        }
        HashMap<String, String> searchMap = new HashMap<>();
        for (String value : split) {
            String[] result = value.split("=");
            if(result.length>1){
                searchMap.put(result[0],result[1]);
            }else {
                searchMap.put(result[0],"");
            }
        }
        searchMap.keySet().forEach(key->{
            String simple = simpleMap.get(key);
            String search = searchMap.get(key);
            System.out.println(simple+"\t"+search+"\t"+key);
        });

        System.out.println("25-50k".split("k").length);
    }
}
