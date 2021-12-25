import java.awt.image.BufferedImage
import java.awt.Graphics2D
import Highlight.*
import scala.collection.mutable.ArrayBuffer
import scala.util.hashing.Hashing.Default
class PlayerController(player: Player)(using ctx: Game) {
    var currentState: ControllerState = new DefaultState
    val board = ctx.board
    val hand = ctx.testHand
    val mana = player.reserve
    var absPos = ctx.window.mouseManager.pos
    var mousePos = ((absPos(0)-board.x)/board.cardSize, (absPos(1)-board.y)/board.cardSize)
    
    var cardContainer: Option[Card] = None
    val dragTimer = new Timer(250)
    val testTimer = new Timer(500)
    var toHighlight = ArrayBuffer[(Graphics2D) => Unit]()



    def tick(): Unit = {
        updateControlls()
        currentState.tick()
    }

   
    def draw(g2d: java.awt.Graphics2D): Unit = {
        if(board.contains(absPos)) then g2d.highlightCardOverBoard(RedHL, absPos)
        if(hand.contains(absPos)) then g2d.highlightCardOverHand(WhiteHL, absPos)
        currentState.draw(g2d)


    }


    def updateControlls(): Unit = {
        absPos = ctx.window.mouseManager.pos
        mousePos = board.getMouseQuadrant()
    }

    abstract class ControllerState {
        def tick(): Unit
        def draw(g2d: Graphics2D): Unit
    }

    case class DefaultState() extends ControllerState {
       
        def tick(): Unit = {
            handDebug()
            if(isValidBoardDrag)
                enterBoardDragState()
            else if(isValidHandDrag)
                enterHandDragState()
         
        }

        def draw(g2d: Graphics2D): Unit = {

        }


            def isValidBoardDrag: Boolean = {ctx.window.mouseManager.leftPressed && board.isCard(mousePos) && cardContainer == None && dragTimer()}
            def isValidHandDrag: Boolean = {ctx.window.mouseManager.leftPressed && hand.bound.contains(absPos) && cardContainer == None && dragTimer()}
    }

    case class DraggingState() extends ControllerState {
        var isAllowedToRelease = false
        def tick(): Unit = {
            if(!ctx.window.mouseManager.leftPressed)
                isAllowedToRelease = true
            
            if(ctx.window.mouseManager.leftPressed && isAllowedToRelease && board.isNotCard(mousePos)){
                board.set(mousePos, cardContainer)
                cardContainer = None
                currentState = DefaultState()
                dragTimer.reset()
            }

            //if(currentState == this)
                //toHighlight += {_.drawImage(cardContainer.get.image, absPos(0), absPos(1), 100, 100, null)}
        }

        def draw(g2d: Graphics2D): Unit = {
            cardContainer.get.drawCard(g2d, absPos(0), absPos(1), 100, 100)
        }
    }

    case class HandDraggingState() extends ControllerState {
        var insertBound = if(hand.cards.indices.length > 0) then (0 to hand.cards.indices.last+1) else 0 to 0
        var isAllowedToRelease = false
        require(cardContainer.isDefined, "CARD CONTAINER NOT DEFINED")
        def tick(): Unit = {
            if(!ctx.window.mouseManager.leftPressed)
                isAllowedToRelease = true
            if(isValidBoardInsert)
                board.set(mousePos, cardContainer)
                cardContainer = None
                currentState = DefaultState()
                dragTimer.reset()
            else if(isValidHandInsert){
                hand.insert(hand.getInsertIndex(absPos), cardContainer.get)
                cardContainer = None
                currentState = DefaultState()
                dragTimer.reset()
            }
        }

        def draw(g2d: Graphics2D): Unit = {
            if(isValidInsertPos)
                hand.highlightInsert(g2d, absPos)
            g2d.drawImage(cardContainer.get.image, absPos(0), absPos(1), 100, 100, null)
        }

        def isValidInsertPos: Boolean = insertBound.contains(hand.getInsertIndex(absPos)) && (0 until hand.cardY).contains(absPos(1))
        def isValidHandInsert: Boolean = isValidInsertPos && isAllowedToRelease && ctx.window.mouseManager.leftPressed
        def isValidBoardInsert: Boolean = ctx.window.mouseManager.leftPressed && isAllowedToRelease && board.isNotCard(mousePos)
    }

    def handDebug(): Unit = {
        if(ctx.window.keyManager.isKeyPressed(65) && testTimer.resetIf())
                hand.drawCard()
        if(ctx.window.keyManager.isKeyPressed(68) && testTimer.resetIf())
            cardContainer = Some(hand.cards(0))
            hand.cards -= hand.cards(0)
            currentState = DraggingState()
    }

    def enterBoardDragState(): Unit = {
        cardContainer = board(mousePos)
        board.set(mousePos, None)
        currentState = DraggingState()
    }

    def enterHandDragState(): Unit = {
        cardContainer = Some(hand.cards(hand.getMouseQuadrant(absPos)))
        hand.cards -= hand.cards(hand.getMouseQuadrant(absPos))
        currentState = HandDraggingState()
    }

    extension (g2d: Graphics2D){
        /** highlights cards
         * @param hl highlight which contains an image
         * @param pos position in absolute cooridnates
        */
        def highlightCardOverBoard(hl: Highlight, pos: (Int, Int)): Unit = {
            board.highlightSquare(g2d, hl, board.getMouseQuadrant())
        }
        /** takes in absolute position
         * @param hl highlight which contains an image
         * @param pos position in absolute cooridnates
        */
        def highlightCardOverHand(hl: Highlight, pos: (Int, Int)): Unit = {
            hand.highlightCard(g2d, WhiteHL, hand.getMouseQuadrant(pos))
        }
    }

}
enum Highlight(path: String){
    var img = Game.loadImage(path)
    def apply: BufferedImage = img
    def draw(x: Int = 0, y: Int = 0, width: Int = img.getWidth, height: Int = img.getHeight)(using g2d: Graphics2D): Unit = {
        g2d.drawImage(img, x, y, width, height, null)
    }
    case WhiteHL extends Highlight("highlight.png")
    case RedHL extends Highlight("redHighlight.png")
}

enum Team() {
    case Red extends Team
    case Blue extends Team
}
