import styled from "@emotion/styled";

export default function PatchnotesLink() {
    return (
        <Wrapper>
            <a
                href={"https://github.com/WE-Kaito/digimon-tcg-simulator/wiki/Patchnotes#03042026"}
                target="_blank"
                rel="noopener noreferrer"
            >
                Patch notes (03.04.2026)
            </a>
        </Wrapper>
    );
}

const Wrapper = styled.sub`
    width: 100vw;
    position: fixed;
    bottom: -2px;
    left: 114px;
    transform: translateX(-50%);
    font-family: Cousine, monospace;
`;
