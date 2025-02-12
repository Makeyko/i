package org.wf.dp.dniprorada.util.unisender;

import com.mongodb.util.JSON;
import java.io.UnsupportedEncodingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.wf.dp.dniprorada.util.unisender.requests.CreateCampaignRequest;
import org.wf.dp.dniprorada.util.unisender.requests.CreateEmailMessageRequest;
import org.wf.dp.dniprorada.util.unisender.requests.SubscribeRequest;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Dmytro Tsapko on 11/28/2015.
 */
public class UniSender {
    final static private Logger oLog = LoggerFactory.getLogger(UniSender.class);
    final static private String API_URL = "http://api.unisender.com/";
    final static private String SUBSCRIBE_URI = "/api/subscribe";
    final static private String CREATE_EMAIL_MESSAGE_URI = "/api/createEmailMessage";
    final static private String CREATE_CAMPAIGN_URI = "/api/createCampaign";
    final static private String AND = "&";
    final private String sAuthKey;
    final private String sLang;
    private StringBuilder osURL;

    /**
     * @param sAuthKey - api_key - this is access key for UniSender API
     * @param sLang   - LANG language of UniSender API messages
     */
    public UniSender(String sAuthKey, String sLang) {

        if (StringUtils.isBlank(sAuthKey) || StringUtils.isBlank(sLang))
            throw new IllegalArgumentException("Neither the Api key nor lang can't be empty.");

        this.sAuthKey = sAuthKey;
        this.sLang = sLang;

        this.osURL = new StringBuilder(this.API_URL);
        osURL.append(this.sLang);
    }

    /**
     * @param sAuthKey - api_key - this is access key for UniSender API.
     *               LANG parameter will be "EN".
     */
    public UniSender(String sAuthKey) {
        this(sAuthKey, "en");
    }

    /**
     * rest resource described - https://support.unisender.com/index.php?/Knowledgebase/Article/View/57/0/subscribe---podpist-drest-n-odin-ili-neskolko-spiskov-rssylki
     * This method shall add user to mail list using UniSender service
     *
     * @return
     */
    public UniResponse subscribe(SubscribeRequest oSubscribeRequest) {

        MultiValueMap<String, Object> mParam = new LinkedMultiValueMap<String, Object>();

        //mandatory part
        StringBuilder osURL = new StringBuilder(this.osURL);
        osURL.append(SUBSCRIBE_URI);
        mParam.add("format", "json");
        mParam.add("api_key", sAuthKey);
        mParam.add("list_ids", StringUtils.join(oSubscribeRequest.getListIds(), ","));
        //conditionally mandatory
        if (!StringUtils.isBlank(oSubscribeRequest.getEmail()))
            mParam.add("fields[email]", oSubscribeRequest.getEmail());
        if (!StringUtils.isBlank(oSubscribeRequest.getPhone()))
            mParam.add("fields[phone]", oSubscribeRequest.getPhone());
        //optional
        if (oSubscribeRequest.getTags() != null && !oSubscribeRequest.getTags().isEmpty())
            mParam.add("tags", StringUtils.join(
                    oSubscribeRequest.getTags(), ","));
        if (!StringUtils.isBlank(oSubscribeRequest.getRequestIp()))
            mParam.add("request_ip", oSubscribeRequest.getRequestIp());
        if (oSubscribeRequest.getRequestTime() != null)
            mParam.add("request_time", getFormattedDate(
                    oSubscribeRequest.getRequestTime()));
        mParam.add("double_optin", Integer.toString(oSubscribeRequest.getDoubleOptin()));
        if (!StringUtils.isBlank(oSubscribeRequest.getConfirmIp()))
            mParam.add("confirm_ip", oSubscribeRequest.getConfirmIp());
        if (oSubscribeRequest.getConfirmTime() != null)
            mParam.add("confirm_time", getFormattedDate(
                    oSubscribeRequest.getConfirmTime()));
        mParam.add("overwrite", Integer.toString(oSubscribeRequest.getOverwrite()));

        oLog.info("RESULT osURL: {}", osURL.toString());
        oLog.info("RESULT mParam: {}", mParam);

        UniResponse oUniResponse = sendRequest(mParam, osURL.toString(), null);

        oLog.info("RESULT oUniResponse: {}", oUniResponse);

        return oUniResponse;
    }

    /**
     * this method has double_optin equals to 3 and overwrite equals to 1.
     *
     * @param aaID
     * @param sMail
     * @return
     */
    public UniResponse subscribe(List<String> aaID, String sMail) {

        SubscribeRequest oSubscribeRequest = SubscribeRequest.getBuilder(this.sAuthKey, this.sLang)
                .setListIds(aaID)
                .setEmail(sMail)
                .setDoubleOptin(3)
                .setOverwrite(1).build();

        return subscribe(oSubscribeRequest);

    }

    public UniResponse createEmailMessage(String sFromName, String sFromMail, String sSubject, String sBody,
            String sID_List) {
        oLog.info("sSubject: {}", sSubject);
        CreateEmailMessageRequest oCreateEmailMessageRequest = CreateEmailMessageRequest
                .getBuilder(this.sAuthKey, this.sLang)
                .setSenderName(sFromName)
                .setSenderEmail(sFromMail)
                .setSubject(sSubject)
                .setBody(sBody)
                .setListId(sID_List)
                .build();
        oLog.info("!sSubject: {}", oCreateEmailMessageRequest.getSubject());
        return createEmailMessage(oCreateEmailMessageRequest);
    }

    public UniResponse createEmailMessage(CreateEmailMessageRequest oCreateEmailMessageRequest) {

        MultiValueMap<String, Object> mParamObject = new LinkedMultiValueMap<String, Object>();
        MultiValueMap<String, ByteArrayResource> mParamByteArray = new LinkedMultiValueMap<String, ByteArrayResource>();

        //mandatory part
        StringBuilder osURL = new StringBuilder(this.osURL);
        osURL.append(CREATE_EMAIL_MESSAGE_URI);
        mParamObject.add("format", "json");
        mParamObject.add("api_key", sAuthKey);
        mParamObject.add("sender_name", oCreateEmailMessageRequest.getSenderName());
        mParamObject.add("sender_email", oCreateEmailMessageRequest.getSenderEmail());
        mParamObject.add("lang", "ua");
        
        //parametersMap.add("subject", createEmailMessageRequest.getSubject());
        //String subject = createEmailMessageRequest.getSubject() == null || "".equals(createEmailMessageRequest.getSubject()) ? " " : createEmailMessageRequest.getSubject();
        /*mParamByteArray.add("subject", new ByteArrayResource(oCreateEmailMessageRequest.getSubject().getBytes(StandardCharsets.UTF_8)));
        String sBody = oCreateEmailMessageRequest.getSubject() + " | " +  oCreateEmailMessageRequest.getBody();
        oLog.info("!sBody: {}", sBody);
        mParamByteArray.add("body", new ByteArrayResource(sBody.getBytes(StandardCharsets.UTF_8)));*/
        mParamObject.add("list_id", oCreateEmailMessageRequest.getListId());
        //optional
        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getTextBody()))
            mParamObject.add("text_body", oCreateEmailMessageRequest.getTextBody());
        //generate_text has default value == 0
        mParamObject.add("generate_text", Integer.toString(oCreateEmailMessageRequest.getGenerateText()));

        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getTag()))
            mParamObject.add("tag", oCreateEmailMessageRequest.getTag());

        Map<String, ByteArrayResource> mAttachment = oCreateEmailMessageRequest.getAttachments();
        for (String sFileName : mAttachment.keySet()) {
            ByteArrayResource oAttachment = mAttachment.get(sFileName);
            mParamByteArray.add("attachments[" + sFileName + "]", oAttachment);
        }
        mParamByteArray.add("subject", new ByteArrayResource(oCreateEmailMessageRequest.getSubject().getBytes(StandardCharsets.UTF_8)));
        String sBody = /*oCreateEmailMessageRequest.getSubject();// + " | " +*/  oCreateEmailMessageRequest.getBody();
        oLog.info("!sBody: {}", sBody);
        mParamByteArray.add("body", new ByteArrayResource(sBody.getBytes(StandardCharsets.UTF_8)));

        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getLang()))
            //parametersMap.add("lang", createEmailMessageRequest.getLang());
            mParamObject.add("lang", "ua");
        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getSeriesDay()))
            mParamObject.add("series_day", oCreateEmailMessageRequest.getSeriesDay());
        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getSeriesTime()))
            mParamObject.add("series_time", oCreateEmailMessageRequest.getSeriesTime());
        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getWrapType()))
            mParamObject.add("wrap_type", oCreateEmailMessageRequest.getWrapType());
        if (!StringUtils.isBlank(oCreateEmailMessageRequest.getCategories()))
            mParamObject.add("categories", oCreateEmailMessageRequest.getCategories());

        oLog.info("RESULT osURL: {}", osURL.toString());
        oLog.info("RESULT mParamObject: {}", mParamObject);

        UniResponse oUniResponse = sendRequest(mParamObject, osURL.toString(), mParamByteArray);

        oLog.info("RESULT oUniResponse: {}", oUniResponse);

        return oUniResponse;
    }

    public UniResponse createCampaign(CreateCampaignRequest oCreateCampaignRequest, String sToMail) {

        MultiValueMap<String, Object> mParam = new LinkedMultiValueMap<String, Object>();

        //mandatory part
        StringBuilder osURL = new StringBuilder(this.osURL);
        osURL.append(CREATE_CAMPAIGN_URI);
        mParam.add("format", "json");
        mParam.add("api_key", sAuthKey);
        mParam.add("message_id", oCreateCampaignRequest.getMessageId());
        mParam.add("contacts", sToMail);
        oLog.info("RESULT osURL: {}", osURL.toString());
        oLog.info("RESULT mParam: {}", mParam);

        UniResponse oUniResponse = sendRequest(mParam, osURL.toString(), null);

        oLog.info("RESULT oUniResponse: {}", oUniResponse);

        return oUniResponse;
    }
    
    private UniResponse sendRequest(MultiValueMap<String, Object> mParamObject, String sURL,
            MultiValueMap<String, ByteArrayResource> mParamByteArray) {

        StringHttpMessageConverter oStringHttpMessageConverter = new StringHttpMessageConverter();
        HttpMessageConverter<Resource> oHttpMessageConverter = new ResourceHttpMessageConverter();
        FormHttpMessageConverter oFormHttpMessageConverter = new FormHttpMessageConverter();
        oFormHttpMessageConverter.addPartConverter(oHttpMessageConverter);

        RestTemplate oRestTemplate = new RestTemplate(
                Arrays.asList(oStringHttpMessageConverter, oHttpMessageConverter, oFormHttpMessageConverter));
        //restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8"))); //._HeaderItem("charset", "utf-8")
        //let's construct main HTTP entity
        HttpHeaders oHttpHeaders = new HttpHeaders();
        oHttpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        oHttpHeaders.setAcceptCharset(Arrays.asList(new Charset[] { StandardCharsets.UTF_8 }));

        //Let's construct attachemnts HTTP entities
        if (mParamByteArray != null) {
            Iterator<String> osIterator = mParamByteArray.keySet().iterator();
            for (int n = 0; osIterator.hasNext(); n++) {
                String sFileName = osIterator.next();
                HttpHeaders oHttpHeaders_Part = new HttpHeaders();
                oHttpHeaders_Part.setContentType(new MediaType("application", "octet-stream", StandardCharsets.UTF_8));
                //headers.add("Content-type","application/octet-stream;charset=utf-8");
                //partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                List<ByteArrayResource> aByteArray_Part = mParamByteArray.get(sFileName);
                HttpEntity<ByteArrayResource> oByteArray_Part = new HttpEntity<ByteArrayResource>(aByteArray_Part.get(0), oHttpHeaders_Part); //HttpEntity<ByteArrayResource> bytesPart = new HttpEntity<ByteArrayResource>(bars.get(i), partHeaders);
                oLog.info("!sFileName: {}", sFileName);
                mParamObject.add(sFileName, oByteArray_Part);
            }
        }
        //result HTTP Request httpEntity
        oLog.info("!!!!!!!!!!!before send RESULT mParamObject: {}", mParamObject);
        HttpEntity oHttpEntity = new HttpEntity(mParamObject, oHttpHeaders);
        ResponseEntity<String> osResponseEntity = oRestTemplate.postForEntity(sURL, oHttpEntity, String.class);
        oLog.info("RESULT sURL == {}, osResponseEntity(JSON) : {}", sURL, osResponseEntity);
        return getUniResponse(osResponseEntity.getBody());
    }

    private UniResponse getUniResponse(String soSourceJSON) {
        Map<String, Object> mSourceParam = (Map<String, Object>) JSON.parse(soSourceJSON);
        Object oSourceError = mSourceParam.get("error");

        Map<String, String> mReturnError = new HashMap<>();
        Map<String, Object> mReturnParam = Collections.emptyMap();
        List<String> asReturnWarning = new ArrayList<>();

        if (oSourceError != null) {
            mReturnError.put("error", oSourceError.toString());
            mReturnError.put("code", mSourceParam.get("code").toString());
        } else {
            mReturnParam = (Map<String, Object>) mSourceParam.get("result");
            if (mReturnParam == null) {
                mReturnParam = Collections.emptyMap();
            }

            List<Map<String, String>> amReturnWarning = (List<Map<String, String>>) mSourceParam.get("warnings");
            if (amReturnWarning != null) {
                for (Map<String, String> mReturnWarning : amReturnWarning) {
                    if (mReturnWarning != null && !mReturnWarning.isEmpty()) {
                        asReturnWarning.add(mReturnWarning.get(mReturnWarning.keySet().iterator().next()));
                    }
                }
            }
        }
        return new UniResponse(mReturnParam, asReturnWarning, mReturnError);
    }

    private String getFormattedDate(Date oDateRequest) {
        if (oDateRequest == null)
            return StringUtils.EMPTY;

        Calendar oCalendar = Calendar.getInstance();
        oCalendar.setTime(oDateRequest);
        SimpleDateFormat oSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return oSimpleDateFormat.format(oCalendar.getTime());
    }
}

