import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import information.Agent;
import information.Record;
import org.apache.http.HttpResponse;
import webhook.SalesforceMethods;

import java.util.Scanner;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std.defaultInstance;

public class Main {
    private static final DateFormat ISO8601_DATE_FORMAT = com.fasterxml.jackson.databind.util.ISO8601DateFormat.getInstance();

    public static ArrayList<Record> pullDataFromSF() throws Exception {
        String path = Main.class.getClassLoader().getResource("config.properties").getPath();
        path = path.substring(0, path.indexOf("config.properties"));
        SalesforceMethods salesforceMethods = new SalesforceMethods(path);
        HttpResponse ref = null;

        ref = salesforceMethods.getRef("https://eu3.salesforce.com/services/data/v28.0/query/?q=SELECT+Status,Hours_from_case_creation__c,Score_Final__c,Emails_Count__c,CaseNumber,Subscription_Type__c,First_Response_Completed__c+FROM+Case+WHERE+Status='Open%20and%20Prioritized'+LIMIT+300+FOR+VIEW", true);
        org.apache.http.HttpEntity httpEntity = ref.getEntity();
        String result = org.apache.commons.io.IOUtils.toString(httpEntity.getContent(), "UTF-8");
        //foo
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(ISO8601_DATE_FORMAT);
        objectMapper.setVisibilityChecker(defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        objectMapper.enableDefaultTyping(); // default to using DefaultTyping.OBJECT_AND_NON_CONCRETE
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result = result.substring(result.indexOf("["), result.lastIndexOf("}"));

        ArrayList<Record> casesList = objectMapper.readValue(result, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Record.class));
        return casesList;
    }

    public static void main(String[] args) throws Exception {

        ArrayList<Record> casesList = pullDataFromSF();

        List<Record> noIRFirstToAssign = new ArrayList();
        List<Record> q = new ArrayList();
        List<Record> noIROnlyIfAgentAvailable = new ArrayList();
        String paying1 = "paying support";
        String paying2 = "business hours support";
        int hoursLimit = 10;

        // split into 3 Qs - noIRFirstToAssign & q

        for (int i = 0; i < casesList.size(); i++) {
            String sub = casesList.get(i).getSubscriptionType();
            if (sub != null) {
                sub = sub.toLowerCase();
                if (sub.equals(paying1) || sub.equals(paying2)) {
                    if (!casesList.get(i).isIr())
                        if (24 - casesList.get(i).getHours() <= hoursLimit)
                            noIRFirstToAssign.add(casesList.get(i)); //need to assign to current shift
                        else
                            noIROnlyIfAgentAvailable.add(casesList.get(i)); //only if there are free agents
                    else
                        q.add(casesList.get(i)); // for cases WITH IR
                } else
                    q.add(casesList.get(i)); // if Sub is Non Paying
            } else
                q.add(casesList.get(i)); // if Sub is Null
        }

        //Sort by hours (no IR Qs)
        Collections.sort(noIRFirstToAssign, new Comparator<Record>() {
            public int compare(Record r1, Record r2) {
                return (int) ((r2.getHours() - r1.getHours()) * 100);
            }
        });

        Collections.sort(noIROnlyIfAgentAvailable, new Comparator<Record>() {
            public int compare(Record r1, Record r2) {
                return (int) ((r2.getHours() - r1.getHours()) * 100);
            }
        });

        //Sort by score
        Collections.sort(q, new Comparator<Record>() {
            public int compare(Record r1, Record r2) {
                return (int) r2.getScore() - (int) r1.getScore();
            }
        });

        System.out.println("noIRFirstToAssign Q - " + noIRFirstToAssign.size() + " tickets" + '\n');
        System.out.println(noIRFirstToAssign);
        System.out.println('\n');
        System.out.println("Q - " + q.size() + " tickets" + '\n');
        System.out.println(q);
        System.out.println("noIR, Only if there are free agents - " + noIROnlyIfAgentAvailable.size() + " tickets" + '\n');
        System.out.println(noIROnlyIfAgentAvailable);
        System.out.println("Total num of O&P tickets: " + (q.size() + noIRFirstToAssign.size() + noIROnlyIfAgentAvailable.size()) + '\n');

        //Agents input from user
        List<Agent> agentsList = new ArrayList<Agent>();
        Scanner scanner = new Scanner(System.in);
        String agentName = "";
        int numOfTickets = 0;
        String doneInput = "";

        System.out.println("Let's make assignments !" + '\n');
        while (!doneInput.equals("y")) {
            System.out.println("Agent name");
            agentName = scanner.nextLine();
            System.out.println("How many tickets do you want to assign to " + agentName + " ?");
            numOfTickets = Integer.parseInt(scanner.nextLine());
            Agent agent = new Agent(agentName, numOfTickets);
            agentsList.add(agent);
            System.out.println("Done? Press Y to finish, N continue");
            doneInput = scanner.nextLine().toLowerCase();


        }

        //split tickets between agents
        boolean doneAssignments = false;

        boolean doneIRq = false;
        int noIRFirstToAssignIndex = 0;
        int qIndex = 0;
        int fullAgentsCounter = 0;
        int totalToAssign = Agent.totalCasesToAssign(agentsList);
        int assignedCounter = 0;

        for (int i = 0; assignedCounter < totalToAssign || assignedCounter == noIRFirstToAssign.size(); i = i % agentsList.size()) {
            if (i == 0) {
                fullAgentsCounter = 0;
            }
            if (assignedCounter == noIRFirstToAssign.size()) {
                Record Case = noIRFirstToAssign.get(noIRFirstToAssignIndex);
                if (24 - Case.getHours() <= hoursLimit) {
                    if (!agentsList.get(i).isAgentFull()) {
                        agentsList.get(i).addCaseToAgent(Case);
                        Case.setStatus("Assigned");
                        noIRFirstToAssignIndex++;
                    } else {
                        fullAgentsCounter++;
                    }
                    i++;
                } else {
                    doneIRq = true;
                }
            } else {
                Record Case = q.get(qIndex);
                if (!agentsList.get(i).isAgentFull()) {
                    agentsList.get(i).addCaseToAgent(Case);
                    Case.setStatus("Assigned");
                    qIndex++;
                } else
                    fullAgentsCounter++;
                i++;
            }
        }

        /*
        for (int i = 0; fullAgentsCounter < agentsList.size() ; i = i % agentsList.size()) {
            if (i == 0) {
                fullAgentsCounter = 0;
            }
            if (assignedCounter <= totalToAssign || assignedCounter == noIRFirstToAssign.size()) {
                Record Case = noIRFirstToAssign.get(noIRFirstToAssignIndex);
                if (24 - Case.getHours() <= hoursLimit) {
                    if (!agentsList.get(i).isAgentFull()) {
                        agentsList.get(i).addCaseToAgent(Case);
                        Case.setStatus("Assigned");
                        noIRFirstToAssignIndex++;
                    } else {
                        fullAgentsCounter++;
                    }
                    i++;
                } else {
                    doneIRq = true;
                }
            } else {
                Record Case = q.get(qIndex);
                if (!agentsList.get(i).isAgentFull()) {
                    agentsList.get(i).addCaseToAgent(Case);
                    Case.setStatus("Assigned");
                    qIndex++;
                } else
                    fullAgentsCounter++;
                i++;
            }
        }
        */

        for (int i = 0; i < agentsList.size(); i++) {
            System.out.println(agentsList.get(i).toStringCaseList());
        }

        for (int i = 0; i < agentsList.size(); i++) {
            System.out.println(agentsList.get(i).toStringCaseListPrintInRow());
        }


    }
}

//create List of agents
// flag: finish
//
