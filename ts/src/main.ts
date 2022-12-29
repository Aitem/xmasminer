const getDrawingContext = () => {
    const canvas = document.getElementById('game');
    if (canvas instanceof HTMLCanvasElement) {
        return canvas.getContext("2d");
    }
    return null;
}

const drawPlayer = (ctx: CanvasRenderingContext2D) => {
    ctx.beginPath();
    ctx.fillStyle = "red";
    ctx.fillRect(20, 40, 40, 40);
    ctx.closePath();
}

const drawTiles = (ctx: CanvasRenderingContext2D) => {
    ctx.beginPath();
    ctx.fillStyle = ""
}

const ctx = getDrawingContext()!;
drawPlayer(ctx);