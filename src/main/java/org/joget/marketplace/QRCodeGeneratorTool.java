package org.joget.marketplace;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.joget.apps.app.model.AppDefinition;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FileUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.springframework.context.ApplicationContext;

public class QRCodeGeneratorTool extends DefaultApplicationPlugin{
    private final static String MESSAGE_PATH = "messages/QRCodeGeneratorTool";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("processtool.qrcodegeneratortool.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }
    
    @Override
    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("processtool.qrcodegeneratortool.name", getClassName(), MESSAGE_PATH);
    }
    
    @Override
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("processtool.qrcodegeneratortool.desc", getClassName(), MESSAGE_PATH);
    }
 
    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/QRCodeGeneratorTool.json", null, true, MESSAGE_PATH);
    }

    @Override
    public Object execute(Map map) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        AppService appService = (AppService) ac.getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = (String) map.get("formDefId");
        String uploadField = (String) map.get("uploadField");
        String format = (String) map.get("format");
        String url = (String) map.get("url");
        String value = (String) map.get("value");
        String height = (String) map.get("height");
        String width = (String) map.get("width");
        String data = "";

        if (height.equals("")) {
            height = "200";
        }
        if (width.equals("")) {
            width = "200";
        }

        switch(format) {
            case "url": 
                data = url;
                break;
            case "text": 
                data = value;
                break;
            default:
                break;
        }

        String recordId;
        WorkflowAssignment wfAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
        if (wfAssignment != null) {
            recordId = appService.getOriginProcessId(wfAssignment.getProcessId());
        } else {
            recordId = (String)properties.get("recordId");
        }
     
        try {
            String charset = "UTF-8";  
            String filename = "qr-code.png";
            String tableName = appService.getFormTableName(appDef, formDefId);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(new String(data.getBytes(charset), charset), QR_CODE, Integer.parseInt(width), Integer.parseInt(height));
            BufferedImage bfi = MatrixToImageWriter.toBufferedImage(bitMatrix);
           
            String path = FileUtil.getUploadPath(tableName, recordId);
            final File file = new File(path + filename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bfi, "png", baos);
            byte[] bytes = baos.toByteArray();
            FileUtils.writeByteArrayToFile(file, bytes);
       
            FormRowSet set = new FormRowSet();
            FormRow r1 = new FormRow();
            r1.put(uploadField, filename);
            set.add(r1);
            set.add(0, r1);
            appService.storeFormData(appDef.getAppId(), appDef.getVersion().toString(), formDefId, set, recordId);
        } catch(Exception ex){
            LogUtil.error(getClassName(), ex, ex.getMessage());
        }
        return null;
    }
}


