package dfa;

import dfa.TTT.DFATTT;
import dfa.data.DFASULReader;
import dfa.data.utils.DFAConstants;
import dfa.dynamicTTT.DFADynamicTTT;
import dfa.modelLearning.DFATeacher;
import generic.data.ResultWriter;
import generic.modelLearning.EQMethod;
import generic.modelLearning.ModelLearningInfo;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.visualization.DefaultVisualizationHelper;
import net.automatalib.visualization.Visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static dfa.data.utils.DFAConstants.BASE_BENCHMARK_PATH;


public class Main {
    public static void main(String[] args) throws Exception {
        ResultWriter writer = new ResultWriter();
        List<ModelLearningInfo> results;

        String basePath = "results/dfa/data";
        String[] methods = {
                "/random_learnLib",
                "/change_tail_learnLib",
                "/remove_alphabet_learnLib",
                "/add_alphabet_learnLib",
                "/remove_state_learnLib",
                "/add_state_learnLib",
        };
        EQMethod eqMethod = EQMethod.WP;
        for (String method : methods) {

            results = run(method, 5, eqMethod, false);
            writer.toCSV(results, basePath + "/" + eqMethod + method + "/0005s_10a.csv");

            results = run(method, 10, eqMethod, false);
            writer.toCSV(results, basePath + "/" + eqMethod + method + "/0010s_10a.csv");

            results = run(method, 50, eqMethod, false);
            writer.toCSV(results, basePath + "/" + eqMethod + method + "/0050s_10a.csv");

            results = run(method, 100, eqMethod, false);
            writer.toCSV(results, basePath + "/" + eqMethod + method + "/0100s_10a.csv");

        }
    }

    public static List<ModelLearningInfo> run(String method, int stateNum, EQMethod eqOption, Boolean visualize) {
        List<ModelLearningInfo> results = new ArrayList<>();

        long start, end;
        String p, state;

        int id = 0;

        out:
        for (int i = 0; i < 20; i++) {
            for (int j = 1; j < 10; j++) {
                try {
                    id++;
                    state = "/s_" + String.format("%04d", stateNum);
                    p = "/p_" + String.format("%03d", i);


                    //dfa.TTT for outdated SUL
                    String outdatedPath = BASE_BENCHMARK_PATH + method + state + p + "/v_000.dot";
                    File f = new File(outdatedPath);
                    System.out.println("dfa/TTT" + outdatedPath);
                    CompactDFA<String> outdatedDFA = new DFASULReader().parseModelFromDot(f);

                    DFATTT<String> tttLearner = new DFATTT<>(
                            new DFATeacher<>(outdatedDFA, eqOption, true),
                            outdatedDFA.getInputAlphabet()
                    );
                    DFA<?, String> hypothesis = tttLearner.learn();
                    if (visualize)
                        Visualization.visualize(
                                outdatedDFA,
                                outdatedDFA.getInputAlphabet(),
                                new DefaultVisualizationHelper<>()
                        );


                    //Dynamic dfa.TTT
                    String updatedPath = BASE_BENCHMARK_PATH + method + state + p + "/v_" + String.format("%03d", j) + ".dot";
                    f = new File(updatedPath);
                    System.out.println("Dynamic dfa.TTT" + updatedPath);
                    CompactDFA<String> updatedDFA = new DFASULReader().parseModelFromDot(f);
                    DFATeacher<String> teacher = new DFATeacher<>(updatedDFA, eqOption, true);
                    teacher.mqOracle.getCount();
                    if (visualize)
                        Visualization.visualize(
                                updatedDFA,
                                updatedDFA.getInputAlphabet(),
                                new DefaultVisualizationHelper<>()
                        );
                    DFADynamicTTT<String> dynamicTTTLearner = new DFADynamicTTT<>(
                            teacher,
                            tttLearner.getSpanningTree(),
                            tttLearner.getDiscriminationTree(),
                            updatedDFA.getInputAlphabet(),
                            new CompactDFA<>(updatedDFA.getInputAlphabet()),
                            visualize
                    );
                    start = getCurrentTimestamp();
                    DFA<?, String> updatedHypothesis = dynamicTTTLearner.learn();
                    end = getCurrentTimestamp();
                    long dMQ = teacher.getMQCount();
                    long dEQ = dynamicTTTLearner.getEQCounter();
                    System.out.println(dEQ + ", " + dMQ);
                    results.add(new ModelLearningInfo(
                            dMQ, dEQ, updatedHypothesis.getStates().size(), DFAConstants.ALPHABET_SIZE, j,
                            "dfa/dynamicTTT", String.format("s%d_%d", stateNum, id), end - start)
                    );


                    //dfa.TTT for updated sul
                    System.out.println("TTT2       " + updatedPath);
                    updatedDFA = new DFASULReader().parseModelFromDot(f);
                    teacher = new DFATeacher<>(updatedDFA, eqOption, true);
                    teacher.mqOracle.getCount();
                    start = getCurrentTimestamp();
                    DFATTT<String> tttLearner2 = new DFATTT<>(teacher, updatedDFA.getInputAlphabet());
                    DFA<?, String> hyp = tttLearner2.learn();
                    end = getCurrentTimestamp();
                    long MQ = teacher.getMQCount();
                    long EQ = tttLearner2.getEQCounter();
                    System.out.println(EQ + ", " + MQ);
                    results.add(new ModelLearningInfo(
                            MQ, EQ, hyp.getStates().size(), DFAConstants.ALPHABET_SIZE, j,
                            "dfa/TTT", String.format("s%d_%d", stateNum, id), end - start)
                    );

                } catch (FileNotFoundException e) {
                    continue out;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(BASE_BENCHMARK_PATH + method + "/s" + stateNum + "/p" + i + "/v_" + String.format("%03d", j) + ".dot");
                    continue out;
                }
            }
        }
        return results;
    }

    private static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

}
