// On my honor, I have neither given nor received any unauthorized aid on this assignment.

import java.io.*;
import java.util.*;

public class Vsim {
    static int cycleNum=1;
    static boolean isBreak=false;
    static boolean addressChange=false;
    static boolean fetchedBreak=false;
    static boolean isStall=false;
    static int currAddress=256;
    static int prevAddress=-1;
    static int dataStartAddress=0;
    static List<String> disassemblyList = new ArrayList<>();
    static List<String> simulationList = new ArrayList<>();
    static Map<String,String>rOpMap=new HashMap<>();
    static Map<String,String>iOpMap=new HashMap<>();
    static Map<String,String>sOpMap=new HashMap<>();
    static Map<String,String>uOpMap=new HashMap<>();
    static Map<String,Integer>regMap=new HashMap<>();
    static Map<Integer,Integer>dataMap=new HashMap<>();
    static Map<Integer,String>instrMap=new HashMap<>();
    static Queue<String>oldPreIssueQueue=new LinkedList<>();
    static Queue<String>newPreIssueQueue=new LinkedList<>();
    static Queue<String>oldPreAlu1Queue=new LinkedList<>();
    static Queue<String>newPreAlu1Queue=new LinkedList<>();
    static Queue<String>oldPreAlu2Queue=new LinkedList<>();
    static Queue<String>newPreAlu2Queue=new LinkedList<>();
    static Queue<String>oldPreAlu3Queue=new LinkedList<>();
    static Queue<String>newPreAlu3Queue=new LinkedList<>();
    static Queue<String>oldPreMemQueue=new LinkedList<>();
    static Queue<String>newPreMemQueue=new LinkedList<>();
    static Queue<String>oldPostAlu2Queue=new LinkedList<>();
    static Queue<String>newPostAlu2Queue=new LinkedList<>();
    static Queue<String>oldPostAlu3Queue=new LinkedList<>();
    static Queue<String>newPostAlu3Queue=new LinkedList<>();
    static Queue<String>oldPostMemQueue=new LinkedList<>();
    static Queue<String>newPostMemQueue=new LinkedList<>();
    static Queue<String>waitingQueue=new LinkedList<>();
    static Queue<String>executedQueue=new LinkedList<>();
    static Set<String>branchInstructions=new HashSet<>();
    public static void main(String[] args) {
        try {
            String inputFileName=args[0];
            FileReader reader = new FileReader(inputFileName);
            BufferedReader bufferedReader = new BufferedReader(reader);
 
            String line;
            List<String>instructions=new ArrayList<>();
 
            while ((line = bufferedReader.readLine()) != null) {
                instructions.add(line);
            }
            reader.close();
            generateDisassembly(instructions);
            currAddress=256;
            generateSimulation();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void generateDisassembly(List<String> instructions){
        Map<String,String>instrType=new HashMap<>();
        instrType.put("00","S-type");
        instrType.put("01","R-type");
        instrType.put("10","I-type");
        instrType.put("11","U-type");
        createROpMap(rOpMap);
        createIOpMap(iOpMap);
        createSOpMap(sOpMap);
        createUOpMap(uOpMap);
        for(String instruction:instructions){
            if(isBreak){
                generateData(instruction);
                continue;
            }
            String key=instruction.substring(30);
            String type=instrType.get(key);
            switch(type){
                case "S-type":
                    generateSType(instruction);
                    break;
                case "R-type":
                    generateRType(instruction);
                    break;
                case "I-type":
                    generateIType(instruction);
                    break;
                default:
                    generateUType(instruction);
                
            }
        }
    }
    public static void writeToFile(String filename, List<String>list){
        try {
            FileWriter writer = new FileWriter(filename, true);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for(String str:list){
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createROpMap(Map<String,String>map){
        map.put("00000","add");
        map.put("00001","sub");
        map.put("00010","and");
        map.put("00011","or");
    }
    public static void createIOpMap(Map<String,String>map){
        map.put("00000","addi");
        map.put("00001","andi");
        map.put("00010","ori");
        map.put("00011","sll");
        map.put("00100","sra");
        map.put("00101","lw");
    }
    public static void createSOpMap(Map<String,String>map){
        map.put("00000","beq");
        map.put("00001","bne");
        map.put("00010","blt");
        map.put("00011","sw");

    }
    public static void createUOpMap(Map<String,String>map){
        map.put("00000","jal");
        map.put("11111","break");
    }
    public static void generateSType(String instruction){
        String opcode = instruction.substring(25,30);
        String pnemonic = sOpMap.get(opcode);
        String immediate1=instruction.substring(0,7);
        String rs2="x"+Integer.parseInt(instruction.substring(7,12),2);
        String rs1="x"+Integer.parseInt(instruction.substring(12, 17),2);
        String immediate2=instruction.substring(20,25);
        String immediate=immediate1+immediate2;
        int imm=immediate.charAt(0)=='1'?-calculateTwosComplement(immediate):Integer.parseInt(immediate,2);
        String decode="";
        if(pnemonic.equals("sw")){
            decode=String.format("%s %s, %d(%s)",pnemonic,rs1,imm,rs2);
        }
        else{
            decode=String.format("%s %s, %s, #%d",pnemonic,rs1,rs2,imm);
        }
        disassemblyList.add(String.format("%s\t%d\t%s",instruction,currAddress,decode));
        currAddress+=4;

    }
    public static void generateRType(String instruction){
        String opcode = instruction.substring(25,30);
        String pnemonic = rOpMap.get(opcode);
        String rs2="x"+Integer.parseInt(instruction.substring(7,12),2);
        String rs1="x"+Integer.parseInt(instruction.substring(12, 17),2);
        String rd="x"+Integer.parseInt(instruction.substring(20,25),2);
        String decode = pnemonic+" "+rd+", "+rs1+", "+rs2;
        disassemblyList.add(String.format("%s\t%d\t%s",instruction,currAddress,decode));
        currAddress+=4;
    }
    public static void generateIType(String instruction){
        String opcode = instruction.substring(25,30);
        String pnemonic = iOpMap.get(opcode);
        String immediate = instruction.substring(0,12);
        String rs1="x"+Integer.parseInt(instruction.substring(12,17),2);
        String rd="x"+Integer.parseInt(instruction.substring(20,25),2);
        int imm=immediate.charAt(0)=='1'?-calculateTwosComplement(immediate):Integer.parseInt(immediate,2);
        String decode="";
        if(pnemonic.equals("lw")){
            decode=String.format("%s %s, %d(%s)",pnemonic,rd,imm,rs1);
        }
        else{
            decode=String.format("%s %s, %s, #%d",pnemonic,rd,rs1,imm);
        }
        disassemblyList.add(String.format("%s\t%d\t%s",instruction,currAddress,decode));
        currAddress+=4;

    }
    public static void generateUType(String instruction){
        String opcode = instruction.substring(25,30);
        String pnemonic = uOpMap.get(opcode);
        String decode="";
        if(pnemonic.equals("break")){
            decode="break";
            isBreak=true;
        }
        else{
            String immediate = instruction.substring(0,20);
            String rd="x"+Integer.parseInt(instruction.substring(20,25),2);
            int imm=immediate.charAt(0)=='1'?-calculateTwosComplement(immediate):Integer.parseInt(immediate,2);
            decode=String.format("%s %s, #%d",pnemonic,rd,imm);
        }
        disassemblyList.add(String.format("%s\t%d\t%s",instruction,currAddress,decode));
        currAddress+=4;

    }
    public static void generateData(String instruction){
        int data=instruction.charAt(0)=='1'?-calculateTwosComplement(instruction):Integer.parseInt(instruction,2);
        String decode=data+"";
        disassemblyList.add(String.format("%s\t%d\t%s",instruction,currAddress,decode));
        currAddress+=4;
    }
    public static int calculateTwosComplement(String str){
        String oneComp="";
        for(int i=0;i<str.length();++i){
            char bit=str.charAt(i);
            oneComp+=(bit=='0')?"1":"0";
        }
        String b="1";
        int i=oneComp.length()-1,j=0,carry=0;
        StringBuilder sb=new StringBuilder();
        while(i>=0 || j>=0){
            int sum = carry;
            if (j >= 0) sum += b.charAt(j--) - '0';
            if (i >= 0) sum += oneComp.charAt(i--) - '0';
            sb.append(sum % 2);
            carry = sum / 2;
        }
        if (carry != 0) sb.append(carry);
        String result = sb.reverse().toString();
        return Integer.parseInt(result,2);

    }
    public static void generateSimulation(){
        for(int num=0;num<32;++num){
            String key="x"+num;
            regMap.put(key,0);
        }
        initializeDataMap();
        initializeInstrMap();
        initializeBranchInstructions();
        while(true){
            addressChange=false;
            fetch();
            issue();
            alu1();
            alu2();
            alu3();
            mem();
            writeback();
            int count=0;
            simulationList.add("--------------------");
            simulationList.add(String.format("Cycle %d:",cycleNum));
            simulationList.add("");
            simulationList.add("IF Unit:");
            simulationList.add(String.format("\tWaiting: %s",!waitingQueue.isEmpty()?("["+waitingQueue.peek()+"]"):""));
            simulationList.add(String.format("\tExecuted: %s",!executedQueue.isEmpty()?("["+executedQueue.peek()+"]"):""));
            simulationList.add("Pre-Issue Queue:");
            for(String str:newPreIssueQueue){
                simulationList.add(String.format("\tEntry %d: [%s]",count,str));
                count++;
            }
            int tCount=count;
            for(int i=0;i<4-tCount;++i){
                simulationList.add(String.format("\tEntry %d:",count));
                count++;
            }
            count=0;
            simulationList.add("Pre-ALU1 Queue:");
            for(String str:newPreAlu1Queue){
                simulationList.add(String.format("\tEntry %d: [%s]",count,str));
                count++;
            }
            tCount=count;
            for(int i=0;i<2-tCount;++i){
                simulationList.add(String.format("\tEntry %d:",count));
                count++;
            }
            simulationList.add(String.format("Pre-MEM Queue: %s",!newPreMemQueue.isEmpty()?("["+newPreMemQueue.peek()+"]"):""));
            simulationList.add(String.format("Post-MEM Queue: %s",!newPostMemQueue.isEmpty()?("["+newPostMemQueue.peek()+"]"):""));
            simulationList.add(String.format("Pre-ALU2 Queue: %s",!newPreAlu2Queue.isEmpty()?("["+newPreAlu2Queue.peek()+"]"):""));
            simulationList.add(String.format("Post-ALU2 Queue: %s",!newPostAlu2Queue.isEmpty()?("["+newPostAlu2Queue.peek()+"]"):""));
            simulationList.add(String.format("Pre-ALU3 Queue: %s",!newPreAlu3Queue.isEmpty()?("["+newPreAlu3Queue.peek()+"]"):""));
            simulationList.add(String.format("Post-ALU3 Queue: %s",!newPostAlu3Queue.isEmpty()?("["+newPostAlu3Queue.peek()+"]"):""));
            simulationList.add("");
            simulationList.add("Registers");
            simulationList.add(String.format("x00:\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",regMap.get("x0"),regMap.get("x1"),regMap.get("x2"),regMap.get("x3"),regMap.get("x4"),regMap.get("x5"),regMap.get("x6"),regMap.get("x7")));
            simulationList.add(String.format("x08:\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",regMap.get("x8"),regMap.get("x9"),regMap.get("x10"),regMap.get("x11"),regMap.get("x12"),regMap.get("x13"),regMap.get("x14"),regMap.get("x15")));
            simulationList.add(String.format("x16:\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",regMap.get("x16"),regMap.get("x17"),regMap.get("x18"),regMap.get("x19"),regMap.get("x20"),regMap.get("x21"),regMap.get("x22"),regMap.get("x23")));
            simulationList.add(String.format("x24:\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",regMap.get("x24"),regMap.get("x25"),regMap.get("x26"),regMap.get("x27"),regMap.get("x28"),regMap.get("x29"),regMap.get("x30"),regMap.get("x31")));
            simulationList.add("Data");
            int dataAddress=dataStartAddress;
            while(true){
                StringBuilder sb=new StringBuilder();
                int flag=0;
                for(int i=0;i<8;++i){
                    if(dataMap.get(dataAddress)!=null){
                        if(sb.length()==0) sb.append(""+dataAddress+":\t");
                        sb.append(dataMap.get(dataAddress));
                        sb.append("\t");
                        dataAddress+=4;
                    }
                    else{
                        flag=1;
                        break;
                    }
                }
                String s=sb.toString();
                if(!s.equals("")) simulationList.add(s.substring(0,s.length()-1));
                if(flag==1) break;
            }
            cycleNum++;
            if(fetchedBreak) break;
            clone(oldPreIssueQueue,newPreIssueQueue);
            clone(oldPreAlu1Queue,newPreAlu1Queue);
            clone(oldPreMemQueue,newPreMemQueue);
            clone(oldPostMemQueue,newPostMemQueue);
            clone(oldPreAlu2Queue,newPreAlu2Queue);
            clone(oldPostAlu2Queue,newPostAlu2Queue);
            clone(oldPreAlu3Queue,newPreAlu3Queue);
            clone(oldPostAlu3Queue,newPostAlu3Queue);
            while(!executedQueue.isEmpty()) executedQueue.poll();
            if(!isStall && !addressChange) {
                prevAddress=currAddress;
                currAddress+=4;
            }
        }
        writeToFile("simulation.txt",simulationList);
    }
    public static void clone(Queue<String>Old,Queue<String>New){
        while(!Old.isEmpty()) Old.poll();
        Old.addAll(New);
    }
    public static void initializeDataMap(){
        boolean b=false;
        int count=0;
        for(String disassembly:disassemblyList){
            String[] str=disassembly.split("\t");
            if(b){
                int key=Integer.parseInt(str[1]);
                int value=Integer.parseInt(str[2]);
                dataMap.put(key,value);
                if(count==0) dataStartAddress=key;
                count++;
                continue;
            }
            if(str[2].equals("break")){
                b=true;
            }

        }
    }
    public static void initializeInstrMap(){
        for(String disassembly:disassemblyList){
            String[] str=disassembly.split("\t");
            int addr=Integer.parseInt(str[1]);
            if(str[2].equals("break")){
                instrMap.put(addr,str[2]);
                break;
            }
            instrMap.put(addr,str[2]);
        }
    }
    public static void initializeBranchInstructions(){
        branchInstructions.add("beq");
        branchInstructions.add("bne");
        branchInstructions.add("blt");
        branchInstructions.add("jal");
    }
    public static void fetch(){
        if(!isStall){
            if(oldPreIssueQueue.size()==4){
                currAddress=prevAddress;
                return;
            }
            String instr1=instrMap.get(currAddress);
            String[] str1=instr1.split(" ");
            String pnem1=str1[0];
            if(pnem1.equals("break")){
                executedQueue.add(instr1);
                fetchedBreak=true;
            }
            else if(!branchInstructions.contains(pnem1)){
                newPreIssueQueue.add(instr1);
                currAddress+=4;
                String instr2=instrMap.get(currAddress);
                String[] str2=instr2.split(" ");
                String pnem2=str2[0];
                if(pnem2.equals("break")){
                    executedQueue.add(instr2);
                    fetchedBreak=true;
                }
                else if(!branchInstructions.contains(pnem2)){
                    if(newPreIssueQueue.size()<4){
                        newPreIssueQueue.add(instr2);
                    }
                    else currAddress-=4;
                }
                else{
                    if(newPreIssueQueue.size()<4) readBranchInstruction(instr2, str2, pnem2);
                    else currAddress-=4;

                }
            }
            else{
                readBranchInstruction(instr1,str1,pnem1);
            }
        }
        else{
            if(!isBranchDependency(waitingQueue.peek())) {
                executedQueue.add(waitingQueue.poll());
                isStall=false;
                String instruction=executedQueue.peek();
                String[] str=instruction.split(" ");
                String pnem=str[0];
                switch(pnem){
                    case "beq":
                        checkBeq(str);
                        break;
                    case "bne":
                        checkBne(str);
                        break;
                    case "blt":
                        checkBlt(str);
                        break;
                    case "jal":
                        performJal(str);
                        break;
                }
            }
        }
    }
    public static void issue(){
        char[] bitMap=new char[]{'0','0','0'};
        Set<Integer>issuedIndices=new HashSet<>();
        int index=0;
        for(String instr:oldPreIssueQueue){
            String[] iParts = instr.split(" ");
            String pnem=iParts[0];
            if(pnem.equals("lw") || pnem.equals("sw")){
                if(bitMap[0]=='0' && oldPreAlu1Queue.size()<2 && !isIssueDependency(instr,index)){
                    newPreAlu1Queue.add(instr);
                    issuedIndices.add(index);
                    bitMap[0]='1';
                }
            }
            else if(pnem.equals("add") || pnem.equals("sub") || pnem.equals("addi")){
                if(bitMap[1]=='0' && oldPreAlu2Queue.isEmpty() && !isIssueDependency(instr,index)){
                    newPreAlu2Queue.add(instr);
                    issuedIndices.add(index);
                    bitMap[1]='1';
                }
            }
            else{
                if(bitMap[2]=='0' && oldPreAlu3Queue.isEmpty() && !isIssueDependency(instr,index)){
                    newPreAlu3Queue.add(instr);
                    bitMap[2]='1';
                    issuedIndices.add(index);
                }
            }
            index++;

        }
        Queue<String>temp=new LinkedList<>();
        index=0;
        for(String instr:newPreIssueQueue){
            if(!issuedIndices.contains(index)) temp.add(instr);
            index++; 
        }
        clone(newPreIssueQueue,temp);
    }
    public static void alu1(){
        if(!oldPreAlu1Queue.isEmpty()) {
            newPreMemQueue.add(oldPreAlu1Queue.peek());
            newPreAlu1Queue.poll();
        }
    }
    public static void alu2(){
        if(!oldPreAlu2Queue.isEmpty()){
            newPostAlu2Queue.add(oldPreAlu2Queue.peek());
            newPreAlu2Queue.poll();
        }
    }
    public static void alu3(){
        if(!oldPreAlu3Queue.isEmpty()){
            newPostAlu3Queue.add(oldPreAlu3Queue.peek());
            newPreAlu3Queue.poll();
        }
    }
    public static void mem(){
        if(!oldPreMemQueue.isEmpty()){
            String instruction=oldPreMemQueue.peek();
            String[] str=instruction.split(" ");
            String pnem=str[0];
            if(pnem.equals("lw")){
                newPostMemQueue.add(oldPreMemQueue.peek());
                newPreMemQueue.poll();
            }
            else{
                performStore(str);
                newPreMemQueue.poll();
            }
        }
    }
    public static void writeback(){
        if(!oldPostMemQueue.isEmpty()){
            newPostMemQueue.poll();
            String instruction = oldPostMemQueue.peek();
            String[] str=instruction.split(" ");
            String pnemonic=str[0];
            performOperation(pnemonic,str);
        }
        if(!oldPostAlu2Queue.isEmpty()){
            newPostAlu2Queue.poll();
            String instruction = oldPostAlu2Queue.peek();
            String[] str=instruction.split(" ");
            String pnemonic=str[0];
            performOperation(pnemonic,str);
        }
        if(!oldPostAlu3Queue.isEmpty()){
            newPostAlu3Queue.poll();
            String instruction = oldPostAlu3Queue.peek();
            String[] str=instruction.split(" ");
            String pnemonic=str[0];
            performOperation(pnemonic,str);
        }

    }
    public static void performOperation(String pnemonic,String[] str){
        switch(pnemonic){
            case "add":
                performAdd(str);
                break;
            case "sub":
                performSub(str);
                break;
            case "and":
                performAnd(str);
                break;
            case "or":
                performOr(str);
                break;
            case "addi":
                performAddI(str);
                break;
            case "andi":
                performAndI(str);
                break;
            case "ori":
                performOrI(str);
                break;
            case "sll":
                performSll(str);
                break;
            case "sra":
                performSra(str);
                break;
            case "lw":
                performLoad(str);
                break;
            case "sw":
                performStore(str);
                break;
            case "beq":
                checkBeq(str);
                break;
            case "bne":
                checkBne(str);
                break;
            case "blt":
                checkBlt(str);
                break;
            case "jal":
                performJal(str);
                break;
        }
    }
    public static void readBranchInstruction(String instr,String[] str,String pnem){
        if(isBranchDependency(instr)) {
            waitingQueue.add(instr);
            isStall=true;
        }
        else {
            executedQueue.add(instr);
            switch(pnem){
                case "beq":
                    checkBeq(str);
                    break;
                case "bne":
                    checkBne(str);
                    break;
                case "blt":
                    checkBlt(str);
                    break;
                case "jal":
                    performJal(str);
                    break;
            }
        }
    }
    public static void performAdd(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        String src2=str[3];
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) + (regMap.get(src2)!=null ? regMap.get(src2) : 0));
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performSub(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        String src2=str[3];
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) - (regMap.get(src2)!=null ? regMap.get(src2) : 0));
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performAnd(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        String src2=str[3];
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) & (regMap.get(src2)!=null ? regMap.get(src2) : 0));
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performOr(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        String src2=str[3];
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) | (regMap.get(src2)!=null ? regMap.get(src2) : 0));
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performAddI(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        int imm=Integer.parseInt(str[3].substring(1));
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) + imm);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performAndI(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        int imm=Integer.parseInt(str[3].substring(1));
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) & imm);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performOrI(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        int imm=Integer.parseInt(str[3].substring(1));
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) | imm);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performSll(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        int imm=Integer.parseInt(str[3].substring(1));
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) << imm);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performSra(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(0,str[2].length()-1);
        int imm=Integer.parseInt(str[3].substring(1));
        regMap.put(destKey,(regMap.get(src1)!=null?regMap.get(src1):0) >> imm);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performLoad(String[] str){
        String destKey=str[1].substring(0,str[1].length()-1);
        String src1=str[2].substring(str[2].indexOf("(")+1,str[2].length()-1);
        int imm=Integer.parseInt(str[2].substring(0,str[2].indexOf("(")));
        int regValue=regMap.get(src1)!=null?regMap.get(src1):0;
        int result = dataMap.get(imm+regValue)!=null?dataMap.get(imm+regValue):0;
        regMap.put(destKey,result);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void performStore(String[] str){
        String src=str[1].substring(0,str[1].length()-1);
        int value=regMap.get(src)!=null?regMap.get(src):0;
        String reg=str[2].substring(str[2].indexOf("(")+1,str[2].length()-1);
        int imm=Integer.parseInt(str[2].substring(0,str[2].indexOf("(")));
        int regValue=regMap.get(reg)!=null?regMap.get(reg):0;
        dataMap.put(imm+regValue,value);
        if(!isStall && !addressChange) {
            prevAddress=currAddress;
            currAddress+=4;
            addressChange=true;
        }
    }
    public static void checkBeq(String[] str){
        String src1=str[1].substring(0,str[1].length()-1);
        String src2=str[2].substring(0,str[2].length()-1);
        int offset=Integer.parseInt(str[3].substring(1));
        int value1=regMap.get(src1)!=null?regMap.get(src1):0;
        int value2=regMap.get(src2)!=null?regMap.get(src2):0;
        if(!isStall && !addressChange){
            prevAddress=currAddress;
            if(value1==value2){
                currAddress=currAddress+(offset<<1);
            }
            else currAddress+=4;
            addressChange=true;
        }
    }
    public static void checkBne(String[] str){
        String src1=str[1].substring(0,str[1].length()-1);
        String src2=str[2].substring(0,str[2].length()-1);
        int offset=Integer.parseInt(str[3].substring(1));
        int value1=regMap.get(src1)!=null?regMap.get(src1):0;
        int value2=regMap.get(src2)!=null?regMap.get(src2):0;
        if(!isStall && !addressChange){
            prevAddress=currAddress;
            if(value1!=value2){
                currAddress=currAddress+(offset<<1);
            }
            else currAddress+=4;
            addressChange=true;
        }
    }
    public static void checkBlt(String[] str){
        String src1=str[1].substring(0,str[1].length()-1);
        String src2=str[2].substring(0,str[2].length()-1);
        int offset=Integer.parseInt(str[3].substring(1));
        int value1=regMap.get(src1)!=null?regMap.get(src1):0;
        int value2=regMap.get(src2)!=null?regMap.get(src2):0;
        if(!isStall && !addressChange){
            prevAddress=currAddress;
            if(value1<value2){
                currAddress=currAddress+(offset<<1);
            }
            else currAddress+=4;
            addressChange=true;
        }
    }
    public static void performJal(String[] str){
        String reg=str[1].substring(0,str[1].length()-1);
        regMap.put(reg,currAddress+4);
        int offset=Integer.parseInt(str[2].substring(1));
        if(!isStall && !addressChange){
            prevAddress=currAddress;
            currAddress=currAddress+(offset<<1);
            addressChange=true;
        }
    }
    public static boolean isBranchDependency(String instruction){
        String[] str=instruction.split(" ");
        String pnem=str[0];
        if(pnem.equals("jal")){
            String reg=str[1].substring(0,str[1].length()-1);
            for(String instr:oldPreIssueQueue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPreAlu1Queue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPreMemQueue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPostMemQueue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPreAlu2Queue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPostAlu2Queue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPreAlu3Queue){
                if(checkJalDependency(instr,reg)) return true;
            }
            for(String instr:oldPostAlu3Queue){
                if(checkJalDependency(instr,reg)) return true;
            }
            return false;

        }
        else{
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(0,str[2].length()-1);
            for(String instr:oldPreIssueQueue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPreAlu1Queue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPreMemQueue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPostMemQueue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPreAlu2Queue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPostAlu2Queue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPreAlu3Queue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            for(String instr:oldPostAlu3Queue){
                if(checkNonJalDependency(instr,reg1,reg2)) return true;
            }
            return false;
        }
    }
    public static boolean checkJalDependency(String instruction,String reg){
        String[] str=instruction.split(" ");
        String pnem=str[0];
        if(pnem.equals("add") || pnem.equals("sub") || pnem.equals("and") || pnem.equals("or")){
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(0,str[2].length()-1);
            String reg3=str[3];
            if(reg1.equals(reg) || reg2.equals(reg) || reg3.equals(reg)) return true;

        }
        else if(pnem.equals("lw") || pnem.equals("sw")){
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(str[2].indexOf("(")+1,str[2].length()-1);
            if(reg1.equals(reg) || reg2.equals(reg)) return true;
        }
        else{
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(0,str[2].length()-1);
            if(reg1.equals(reg) || reg2.equals(reg)) return true;
        }
        return false;
    }
    public static boolean checkNonJalDependency(String instruction, String reg1, String reg2){
        String[] str=instruction.split(" ");
        String pnem=str[0];
        if(pnem.equals("sw")) return false;
        if(pnem.equals("add") || pnem.equals("sub") || pnem.equals("and") || pnem.equals("or")){
            String reg=str[1].substring(0,str[1].length()-1);
            if(reg1.equals(reg) || reg2.equals(reg)) return true;

        }
        else if(pnem.equals("lw")){
            String reg=str[1].substring(0,str[1].length()-1);
            if(reg1.equals(reg) || reg2.equals(reg)) return true;
        }
        else{
            String reg=str[1].substring(0,str[1].length()-1);
            if(reg1.equals(reg) || reg2.equals(reg)) return true;
        }
        return false;
    }
    public static boolean isIssueDependency(String instruction, int index){
        int currIndex=0;
        for(String instr:oldPreIssueQueue){
            if(currIndex<index){
                if((instr.split(" ")[0].equals("sw") && instruction.split(" ")[0].equals("lw")) || (instr.split(" ")[0].equals("sw") && instruction.split(" ")[0].equals("sw")) || checkDependency(instruction,instr)) return true;
                currIndex++;
            }
            else break;
        }
        for(String instr:oldPreAlu1Queue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPreMemQueue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPostMemQueue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPreAlu2Queue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPostAlu2Queue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPreAlu3Queue){
            if(checkDependency(instruction,instr)) return true;
        }
        for(String instr:oldPostAlu3Queue){
            if(checkDependency(instruction,instr)) return true;
        }
        return false;
    }
    public static boolean checkDependency(String instr1,String instr2){
        if(instr1.split(" ")[0].equals("sw") && instr2.split(" ")[0].equals("sw")) return false;
        List<String>regList1=getRegisters(instr1);
        List<String>regList2=getRegisters(instr2);
        if(instr1.split(" ")[0].equals("sw")){
            if(regList1.get(0).equals(regList2.get(0)) || regList1.get(1).equals(regList2.get(0))){
                return true;
            }
            return false;
        }
        else if(instr2.split(" ")[0].equals("sw")){
            if(regList1.get(0).equals(regList2.get(0)) || regList1.get(0).equals(regList2.get(1))){
                return true;
            }
            return false;
        }
        String reg1=regList1.get(0);
        for(String reg2:regList2){
            if(reg1.equals(reg2)) return true;
        }
        String reg2=regList2.get(0);
        for(String re1:regList1){
            if(re1.equals(reg2)) return true;
        }
        return false;

    }
    public static List<String> getRegisters(String instruction){
        List<String>result=new ArrayList<>();
        String[] str=instruction.split(" ");
        String pnem=str[0];
        if(pnem.equals("add") || pnem.equals("sub") || pnem.equals("and") || pnem.equals("or")){
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(0,str[2].length()-1);
            String reg3=str[3];
            result.add(reg1);
            result.add(reg2);
            result.add(reg3);

        }
        else if(pnem.equals("lw") || pnem.equals("sw")){
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(str[2].indexOf("(")+1,str[2].length()-1);
            result.add(reg1);
            result.add(reg2);
        }
        else{
            String reg1=str[1].substring(0,str[1].length()-1);
            String reg2=str[2].substring(0,str[2].length()-1);
            result.add(reg1);
            result.add(reg2);
        }
        return result;
    }
 
}