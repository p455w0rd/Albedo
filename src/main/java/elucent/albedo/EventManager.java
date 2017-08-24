package elucent.albedo;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import elucent.albedo.event.GatherLightsEvent;
import elucent.albedo.event.LightUniformEvent;
import elucent.albedo.event.ProfilerStartEvent;
import elucent.albedo.event.RenderChunkUniformsEvent;
import elucent.albedo.event.RenderEntityEvent;
import elucent.albedo.event.RenderTileEntityEvent;
import elucent.albedo.gui.GuiAlbedoConfig;
import elucent.albedo.lighting.Light;
import elucent.albedo.lighting.LightManager;
import elucent.albedo.util.RenderUtil;
import elucent.albedo.util.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventManager {
	int ticks = 0;
	boolean postedLights = false;
	boolean precedesEntities = true;
	public static boolean isGui = false;
	String section = "";
	@SubscribeEvent
	public void onProfilerChange(ProfilerStartEvent event){
		section = event.getSection();
		if (ConfigManager.enableLights){
			if (event.getSection().compareTo("terrain") == 0){
				isGui = false;
				precedesEntities = true;
				ShaderUtil.useProgram(ShaderUtil.fastLightProgram);
				int tickLoc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "ticks");
				GL20.glUniform1f(tickLoc, (float)ticks + Minecraft.getMinecraft().getRenderPartialTicks());
				int texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "sampler");
				GL20.glUniform1i(texloc, 0);
				texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightmap");
				GL20.glUniform1i(texloc, 1);
				texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "brightlayer");
				GL20.glUniform1i(texloc, 2);
				int playerPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "playerPos");
				GL20.glUniform3f(playerPos, (float)Minecraft.getMinecraft().player.posX, (float)Minecraft.getMinecraft().player.posY, (float)Minecraft.getMinecraft().player.posZ);
				if (!postedLights){
					LightManager.update(Minecraft.getMinecraft().world);
					ShaderUtil.useProgram(0);
					MinecraftForge.EVENT_BUS.post(new LightUniformEvent());
					ShaderUtil.useProgram(ShaderUtil.fastLightProgram);
					LightManager.uploadLights();
					ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
					tickLoc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "ticks");
					GL20.glUniform1f(tickLoc, (float)ticks + Minecraft.getMinecraft().getRenderPartialTicks());
					texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "sampler");
					GL20.glUniform1i(texloc, 0);
					texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightmap");
					GL20.glUniform1i(texloc, 1);
					texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "brightlayer");
					GL20.glUniform1i(texloc, 2);
					LightManager.uploadLights();
					playerPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "playerPos");
					GL20.glUniform3f(playerPos, (float)Minecraft.getMinecraft().player.posX, (float)Minecraft.getMinecraft().player.posY, (float)Minecraft.getMinecraft().player.posZ);
					int lightPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightingEnabled");
					GL20.glUniform1i(lightPos, GL11.glIsEnabled(GL11.GL_LIGHTING) ? 1 : 0);
					ShaderUtil.useProgram(ShaderUtil.fastLightProgram);
					postedLights = true;
					LightManager.clear();
				}
			}
			if (event.getSection().compareTo("sky") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("litParticles") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("weather") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("entities") == 0){
				if (Minecraft.getMinecraft().isCallingFromMinecraftThread()){
					ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
					int lightPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightingEnabled");
					GL20.glUniform1i(lightPos, 1);
					int fogPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "fogIntensity");
					GL20.glUniform1f(fogPos, Minecraft.getMinecraft().world.provider.getDimensionType() == DimensionType.NETHER ? 0.015625f : 1.0f);
				}
			}
			if (event.getSection().compareTo("blockEntities") == 0){
				if (Minecraft.getMinecraft().isCallingFromMinecraftThread()){
					ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
					int lightPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightingEnabled");
					GL20.glUniform1i(lightPos, 1);
				}
			}
			if (event.getSection().compareTo("outline") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("aboveClouds") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("destroyProgress") == 0){
				ShaderUtil.useProgram(0);
			}
			if (event.getSection().compareTo("translucent") == 0){
				ShaderUtil.useProgram(ShaderUtil.fastLightProgram);
				int texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "sampler");
				GL20.glUniform1i(texloc, 0);
				texloc = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "lightmap");
				GL20.glUniform1i(texloc, 1);
				int playerPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "playerPos");
				GL20.glUniform3f(playerPos, (float)Minecraft.getMinecraft().player.posX, (float)Minecraft.getMinecraft().player.posY, (float)Minecraft.getMinecraft().player.posZ);
			}
			if (event.getSection().compareTo("hand") == 0){
				ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
				int entityPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "entityPos");
				EntityPlayer player = Minecraft.getMinecraft().player;
				GL20.glUniform3f(entityPos, (float)player.posX, (float)player.posY+player.height/2.0f, (float)player.posZ);
				precedesEntities = true;
			}
			if (event.getSection().compareTo("gui") == 0){
				isGui = true;
				ShaderUtil.useProgram(0);
			}
		}
	}
	
	@SubscribeEvent
	public void onRenderEntity(RenderEntityEvent event){
		if (ConfigManager.enableLights){
			if (event.getEntity() instanceof EntityLightningBolt){
				ShaderUtil.useProgram(0);
			}
			else if (section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities")){
				ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
			}
			if (ShaderUtil.currentProgram == ShaderUtil.entityLightProgram){
				int entityPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "entityPos");
				GL20.glUniform3f(entityPos, (float)event.getEntity().posX, (float)event.getEntity().posY+event.getEntity().height/2.0f, (float)event.getEntity().posZ);
				int colorMult = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "colorMult");
				GL20.glUniform4f(colorMult, 1.0f, 1.0f, 1.0f, 0.0f);
				if (event.getEntity() instanceof EntityLivingBase){
					EntityLivingBase e = (EntityLivingBase)event.getEntity();
					if (e.hurtTime > 0 || e.deathTime > 0){
						GL20.glUniform4f(colorMult, 1.0f, 0.0f, 0.0f, 0.3f);
					}
					else {
					}
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onRenderLiving(RenderLivingEvent event){
		if (ConfigManager.enableLights){
			if ((event.getEntity()).isPotionActive(Potion.getPotionFromResourceLocation("glowing"))){
				ShaderUtil.useProgram(0);
			}
			else if (section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities")){
				ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
			}
			if (ShaderUtil.currentProgram == ShaderUtil.entityLightProgram){
				int colorMult = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "colorMult");
				GL20.glUniform4f(colorMult, 1.0f, 1.0f, 1.0f, 0.0f);
			}
			ICustomModelLoader m;
		}
	}
	
	@SubscribeEvent
	public void onRenderTileEntity(RenderTileEntityEvent event){
		if (ConfigManager.enableLights){
			if (event.getEntity() instanceof TileEntityEndPortal || event.getEntity() instanceof TileEntityEndGateway){
				ShaderUtil.useProgram(0);
			}
			else if (section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities")){
				ShaderUtil.useProgram(ShaderUtil.entityLightProgram);
			}
			if (ShaderUtil.currentProgram == ShaderUtil.entityLightProgram){
				int entityPos = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "entityPos");
				GL20.glUniform3f(entityPos, (float)event.getEntity().getPos().getX(), (float)event.getEntity().getPos().getY(), (float)event.getEntity().getPos().getZ());
				int colorMult = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "colorMult");
				GL20.glUniform4f(colorMult, 1.0f, 1.0f, 1.0f, 0.0f);
			}
		}
	}
	
	@SubscribeEvent
	public void onRenderChunk(RenderChunkUniformsEvent event){
		if (ConfigManager.enableLights){
			if (ShaderUtil.currentProgram == ShaderUtil.fastLightProgram){
				BlockPos pos = event.getChunk().getPosition();
				int chunkX = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "chunkX");
				int chunkY = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "chunkY");
				int chunkZ = GL20.glGetUniformLocation(ShaderUtil.currentProgram, "chunkZ");
				GL20.glUniform1i(chunkX, pos.getX());
				GL20.glUniform1i(chunkY, pos.getY());
				GL20.glUniform1i(chunkZ, pos.getZ());
			}
		}
	}
	
	@SubscribeEvent
	public void clientTick(ClientTickEvent event){
		if (event.phase == TickEvent.Phase.START){
			ticks ++;
		}
	}
	
	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event){
		postedLights = false;
		if (Minecraft.getMinecraft().isCallingFromMinecraftThread()){
			GlStateManager.disableLighting();
			ShaderUtil.useProgram(0);
		}
	}
	
	/*@SubscribeEvent
	public void onGatherLights(GatherLightsEvent e){
		if (Minecraft.getMinecraft().player != null){
			e.getLightList().add(new Light((float)TileEntityRendererDispatcher.staticPlayerX,
					(float)TileEntityRendererDispatcher.staticPlayerY+0.1f,
					(float)TileEntityRendererDispatcher.staticPlayerZ,
					1f, 0.125f, 0.125f, 4f, 4));
			e.getLightList().add(new Light((float)TileEntityRendererDispatcher.staticPlayerX,
					(float)TileEntityRendererDispatcher.staticPlayerY+0.1f,
					(float)TileEntityRendererDispatcher.staticPlayerZ,
					1f, 0.5f, 0.125f, 0.75f, 14));
		}
	}*/
}
