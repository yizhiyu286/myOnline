package com.atguigu.gmall.list.service.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuListInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.SkuLsService;
import com.atguigu.gmall.util.RedisConfig;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SkuLsServiceImpl implements SkuLsService{
    @Autowired
    private JestClient jestClient;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public void saveSkuInfoToEs(SkuListInfo skuListInfo) {
        Index index = new Index.Builder(skuListInfo).index("gmall").type("SkuInfo").id(skuListInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult getBySkuLsParams(SkuLsParams skuLsParams) {
        String query = "";
        SkuLsResult skuLsResult = new SkuLsResult();
        //拼接查询条件
        // 创建查询bulid
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建查询对象，存放查询及过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //封装查询条件
        if(skuLsParams.getKeyword() != null ){
            MatchQueryBuilder skuName = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(skuName);
            //高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.field("skuName");
            highlightBuilder.postTags("</span>");
            //高亮封装完毕
            searchSourceBuilder.highlight(highlightBuilder);
        }

        //封装过滤条件
        //三级分类id
        if(skuLsParams.getCatalog3Id() != null){
            TermQueryBuilder catalog3Id = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(catalog3Id);
        }

        //平台属性
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            String[] valueIds = skuLsParams.getValueId();
            for (int i = 0; i < valueIds.length; i++) {
                String valueId = valueIds[i];
                TermQueryBuilder queryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(queryBuilder);
            }
        }
        //查询过滤封装完毕
        searchSourceBuilder.query(boolQueryBuilder);
        //价格范围
        //boolQueryBuilder.filter(new RangeQueryBuilder("price").gte(333).lt(555));
        //分页
        searchSourceBuilder.from((skuLsParams.getPageNo()-1)*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //根据热度排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("groupByValueId").field("skuAttrValueList.valueId").size(1000));
        //将所有条件转换成一个字符串对象
        query = searchSourceBuilder.toString();
        //创建用于查询的对象
        Search.Builder builder = new Search.Builder(query);
        Search search = builder.addIndex("gmall").addType("SkuInfo").build();
        //执行查询语句
        try {
            SearchResult searchResult = jestClient.execute(search);
            //System.out.println(searchResult.toString());
            //获取封装好的sku集合的相关信息
            //创建集合存放sku集合
            List<SkuListInfo> skuListInfos = new ArrayList<>();
            List<SearchResult.Hit<SkuListInfo, Void>> searchResultHits = searchResult.getHits(SkuListInfo.class);
            for (SearchResult.Hit<SkuListInfo, Void> hit : searchResultHits) {
                //获取每一个sku
                SkuListInfo source = hit.source;
                //设置高亮显示字段
                if(hit.highlight != null && hit.highlight.size()>0){
                    List<String> skuName = hit.highlight.get("skuName");
                    source.setSkuName(skuName.get(0));
                }
                //设置到返回结果中
                skuListInfos.add(source);
            }
            skuLsResult.setSkuLsInfoList(skuListInfos);
            //设置分页相关信息
            //总记录数
            Long total = searchResult.getTotal();
            //计算总页数
            Long totalPages = (total+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
            //System.out.println("===================="+total+"=========="+totalPages+"===="+skuLsParams.getPageNo());
            //封装
            skuLsResult.setTotal(total);
            skuLsResult.setTotalPages(totalPages);
            //封装平台属性值id
            TermsAggregation valueIds = searchResult.getAggregations().getTermsAggregation("groupByValueId");
            //创建一个集合存放平台属性值id
            ArrayList<String> valueIdList = new ArrayList<>();
            if(valueIds != null){
                List<TermsAggregation.Entry> buckets = valueIds.getBuckets();
                for (TermsAggregation.Entry bucket : buckets) {
                    String valueId = bucket.getKey();
                    valueIdList.add(valueId);
                }
            }
            //封装
            skuLsResult.setAttrValueIdList(valueIdList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return skuLsResult;
    }

    @Override
    public void incrHotscore(String skuId) {
        //增加hotscore
        Jedis jedis = redisUtil.getJedis();
        //拼接存储hotscore的key
        String hotscoreKey = "sku:"+skuId+":hotscore";
        Long hotscore = jedis.incr(hotscoreKey);
        //判断，每增加10写入es中
        if(hotscore%10 == 0){
            //写入es中
            updateHotScore(skuId,hotscore);
        }
    }

    private void updateHotScore(String skuId,Long hotscore){
        //拼接修改语句
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotscore+"\n" +
                "   }\n" +
                "}";
        //创建修改的对象
        Update update = new Update.Builder(updateJson).index("gmall").type("SkuInfo").id(skuId).build();
        //执行
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
