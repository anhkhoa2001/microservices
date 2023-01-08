package org.micro.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.micro.constant.ResourcePath;
import org.micro.dto.RabbitMapper;
import org.micro.dto.RabbitMqType;
import org.micro.util.StringUtil;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
public class GatewayConfig {

    //Typesafe config
    public static final String CONFIG_DIR = System.getProperty("user.dir") + File.separator + "gateway"
                                    + File.separator + "config" + File.separator;

    //Constant root api path
    public static final String API_ROOT_PATH = ResourcePath.API + "/";
    
    //Time out khi call RPC (s)
    public static Integer RPC_TIMEOUT = 5;
    
    //List mã dịch vụ
    public static List<String> SERVICE_LIST = new ArrayList<>();
    
    //Map chứa các key,value ngoài các map định nghĩa bên dưới
    public static final Map<String, String> SERVICE_MAP = new HashMap<>();
    
    //Map chứa list path
    public static final Map<String, RabbitMapper> SERVICE_PATH_MAP = new HashMap<>();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try {
            System.out.println("======= Loading GatewayConfig config... =======");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(new FileInputStream(CONFIG_DIR + "gateway.properties"), "UTF-8"));
            Enumeration e = properties.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = properties.getProperty(key);
                
                System.out.println("+ key: " + key + " => value: " + value);
                if (key.equalsIgnoreCase("rpc.timeout")) {
                    if(StringUtil.isNumberic(value)){
                        RPC_TIMEOUT = Integer.parseInt(value.trim());
                    }
                } else if (key.equalsIgnoreCase("service.list")) {
                    String[] arrDomain = value.split(",");
                    SERVICE_LIST = Arrays.asList(arrDomain);
                } else {
                    SERVICE_MAP.put(key, value);
                    if (key.contains(".path.rabbit")) {
                        loadRabbitJson(value);
                    } else if (key.contains(".path.private")) {
                        if(!value.isEmpty()) {
                            String[] arrPrivatePath = value.split(",");
                            List<String> pathPrivates = Arrays.asList(arrPrivatePath);
                            for(String path:pathPrivates) {
                                RabbitMapper rabbitMapper = SERVICE_PATH_MAP.get(path);
                                if(rabbitMapper != null) {
                                    rabbitMapper.setAccess(true);
                                    SERVICE_PATH_MAP.put(path, rabbitMapper);
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("=== Load GatewayConfig config successfull!!! ===");
        } catch (IOException ex) {
            Logger.getLogger(GatewayConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void loadRabbitJson(String jsonFile) {
        try {
            FileReader reader = new FileReader(CONFIG_DIR + jsonFile + ".json");
            ObjectMapper mapper = new ObjectMapper();
            List<RabbitMqType> rabbitMqTypes = mapper.readValue(reader, new TypeReference<List<RabbitMqType>>() {});
            if (rabbitMqTypes != null && rabbitMqTypes.size() > 0) {
                rabbitMqTypes.forEach((tmp) -> {
                    RabbitMapper rabbitMapper = new RabbitMapper();
                    rabbitMapper.setRabbit_type(tmp.getRabbit());
                    rabbitMapper.setMethod(tmp.getMethod());
                    rabbitMapper.setAccess(false);
                    rabbitMapper.setRole(tmp.getRole());
                    SERVICE_PATH_MAP.put(tmp.getPath(), rabbitMapper);
                });
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GatewayConfig.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GatewayConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}