package information;

import sun.management.resources.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adis on 10/05/2017.
 */
public class Agent {
    String Name;
    int maxNumOfCases;
    List<Record> Cases;
    boolean agentDoneFlag = false;

    //constractor
    public Agent(String Name, int numOfCases) {
        this.Name = Name;
        this.maxNumOfCases = numOfCases;
        this.Cases = new ArrayList<Record>();
    }

    public void addCaseToAgent(Record record) {
        this.Cases.add(record);
    }

    public static int totalCasesToAssign (List <Agent> agentsList){
        int totalCasesToAssign = 0;
        for (Agent agent : agentsList)
        {
            totalCasesToAssign += agent.getAgentsNumOfCases();
        }
        return totalCasesToAssign;

    }

    public int getAgentsNumOfCases() {
        return maxNumOfCases;
    }
    public boolean isAgentFull() {
        return (this.Cases.size() == this.maxNumOfCases);
    }


    public String toString() {
        return Name + ", " + maxNumOfCases + '\n';
    }

    public String toStringCaseList() {
        String str = Name + '\n';
        for (int i = 0; i < Cases.size(); i++) {
            str += Cases.get(i).getCaseNumber().toString() + " IR? " + Cases.get(i).isIr() + '\n';
        }
        return str;
    }

    public String toStringCaseListPrintInRow() {
        String str = Name + '\n';
        for (int i = 0; i < Cases.size(); i++) {
            if(i != Cases.size()-1)
                str += Cases.get(i).getCaseNumber().toString() + ", ";
            else
                str += Cases.get(i).getCaseNumber().toString();
        }
        return str;
    }

}

