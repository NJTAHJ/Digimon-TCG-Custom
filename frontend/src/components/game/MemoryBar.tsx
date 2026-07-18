import styled from "@emotion/styled";
import { WSUtils } from "../../pages/GamePage.tsx";
import { useGameBoardStates } from "../../hooks/useGameBoardStates.ts";
import AddIcon from "@mui/icons-material/AddCircleTwoTone";
import RemoveIcon from "@mui/icons-material/RemoveCircleTwoTone";
import { notifyInfo } from "../../utils/toasts.ts";
import { useGeneralStates } from "../../hooks/useGeneralStates.ts";

export default function MemoryBar({ wsUtils }: { wsUtils: WSUtils }) {
    // ✅ FIXED: Using 'myMemory' to match your store definition
    const myMemory = useGameBoardStates((state) => state.myMemory);
    const setMemory = useGameBoardStates((state) => state.setMemory);
    const usernameTurn = useGameBoardStates((state) => state.usernameTurn);
    const setUsernameTurn = useGameBoardStates((state) => state.setUsernameTurn);

    const playSuspendSfx = useGeneralStates((state) => state.cardWidth * 0.45); // safely get scale sizes

    const isMyTurn = usernameTurn === wsUtils.matchInfo.user;

    const handleMemoryChange = (value: number) => {
        // ✅ FIXED: Using 'myMemory'
        const newMemory = myMemory + value;
        if (newMemory < -10 || newMemory > 10) return;
        setMemory(newMemory);
        wsUtils.sendMessage(`${wsUtils.matchInfo.gameId}:/updateMemory:${newMemory}`);
        
        if (newMemory > 0 && isMyTurn) {
            setUsernameTurn(wsUtils.matchInfo.opponentName);
            wsUtils.sendMessage(`${wsUtils.matchInfo.gameId}:/updatePhase`);
        }
    };

    const handlePassTurn = () => {
        if (!isMyTurn) {
            notifyInfo("It's not your turn!");
            return;
        }
        setMemory(3);
        setUsernameTurn(wsUtils.matchInfo.opponentName);
        wsUtils.sendMessage(`${wsUtils.matchInfo.gameId}:/updateMemory:3`);
        wsUtils.sendMessage(`${wsUtils.matchInfo.gameId}:/updatePhase`);
        wsUtils.sendSfx("playPassTurnSfx");
    };

    return (
        <Container>
            <MemoryTrack>
                {Array.from({ length: 21 }, (_, i) => {
                    const value = i - 10;
                    // ✅ FIXED: Using 'myMemory'
                    const isActive = myMemory === value;
                    return (
                        <MemorySlot key={value} active={isActive} value={value}>
                            {value === 0 ? "0" : Math.abs(value)}
                        </MemorySlot>
                    );
                })}
            </MemoryTrack>
            <ControlContainer>
                <MemoryButton onClick={() => handleMemoryChange(-1)}>
                    <RemoveIcon style={{ fontSize: playSuspendSfx }} />
                </MemoryButton>
                <MemoryButton onClick={() => handleMemoryChange(1)}>
                    <AddIcon style={{ fontSize: playSuspendSfx }} />
                </MemoryButton>
                <PassButton onClick={handlePassTurn}>
                    PASS TURN
                </PassButton>
            </ControlContainer>
        </Container>
    );
}

const Container = styled.div`
    grid-column: 10 / 29;
    grid-row: 10 / 12;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    z-index: 10;
`;

const MemoryTrack = styled.div`
    display: flex;
    width: 100%;
    height: 40px;
    background: #111;
    border: 2px solid #333;
    border-radius: 5px;
    overflow: hidden;
`;

const MemorySlot = styled.div<{ active: boolean; value: number }>`
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: "League Spartan", sans-serif;
    font-size: 16px;
    font-weight: bold;
    color: ${({ active }) => (active ? "#fff" : "#666")};
    background: ${({ active, value }) =>
        active
            ? value === 0
                ? "purple"
                : value < 0
                  ? "crimson"
                  : "royalblue"
            : "transparent"};
    transition: all 0.2s ease;
`;

const ControlContainer = styled.div`
    display: flex;
    gap: 8px;
    align-items: center;
`;

const MemoryButton = styled.button`
    cursor: pointer;
    background: none;
    border: none;
    color: ghostwhite;
    opacity: 0.8;
    &:hover {
        opacity: 1;
        color: royalblue;
    }
    &:active {
        transform: scale(0.95);
    }
`;

const PassButton = styled.button`
    cursor: pointer;
    background: royalblue;
    border: 1px solid dodgerblue;
    border-radius: 4px;
    color: white;
    font-family: "League Spartan", sans-serif;
    font-size: 14px;
    font-weight: bold;
    padding: 4px 12px;
    &:hover {
        background: dodgerblue;
    }
    &:active {
        transform: scale(0.95);
    }
`;