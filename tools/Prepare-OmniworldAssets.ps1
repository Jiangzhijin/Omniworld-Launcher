param([string]$SourceImage = (Join-Path $PSScriptRoot '../HMCL/src/main/resources/assets/img/omniworld-icon.png'))
$ErrorActionPreference = 'Stop'
$assets = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../HMCL/src/main/resources/assets'))
Add-Type -AssemblyName System.Drawing
$imageStream = [IO.MemoryStream]::new([IO.File]::ReadAllBytes([IO.Path]::GetFullPath($SourceImage)))
$source = [Drawing.Image]::FromStream($imageStream)
$iconPath=(Join-Path $assets 'img/omniworld.ico')
$frames=New-Object 'System.Collections.Generic.List[byte[]]'
$sizes=@(16,24,32,48,64,128,256)
foreach($size in $sizes) {
  $bitmap=New-Object Drawing.Bitmap($size,$size)
  $g=[Drawing.Graphics]::FromImage($bitmap)
  $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($source,0,0,$size,$size)
  $stream=New-Object IO.MemoryStream
  $bitmap.Save($stream,[Drawing.Imaging.ImageFormat]::Png)
  $frames.Add($stream.ToArray())
  if($size -eq 256) { $bitmap.Save((Join-Path $assets 'img/omniworld-icon.png'),[Drawing.Imaging.ImageFormat]::Png) }
  $g.Dispose(); $bitmap.Dispose(); $stream.Dispose()
}
$source.Dispose(); $imageStream.Dispose()
$writer=New-Object IO.BinaryWriter([IO.File]::Create($iconPath))
$writer.Write([uint16]0); $writer.Write([uint16]1); $writer.Write([uint16]$sizes.Count)
$offset=6+16*$sizes.Count
for($i=0;$i -lt $sizes.Count;$i++) {
  $writer.Write([byte]($sizes[$i]%256)); $writer.Write([byte]($sizes[$i]%256)); $writer.Write([byte]0); $writer.Write([byte]0)
  $writer.Write([uint16]1); $writer.Write([uint16]32); $writer.Write([uint32]$frames[$i].Length); $writer.Write([uint32]$offset)
  $offset+=$frames[$i].Length
}
foreach($frame in $frames) { $writer.Write($frame) }; $writer.Dispose()
# Short, quiet two-note pluck in standard PCM WAV, playable by Java Sound.
$rate=44100; $count=5292; $samples=New-Object IO.MemoryStream; $pcm=New-Object IO.BinaryWriter($samples)
for($i=0;$i -lt $count;$i++) {
  $t=$i/$rate; $envelope=[Math]::Exp(-$t*42)*[Math]::Min(1,$t*650)
  $value=([Math]::Sin(2*[Math]::PI*1318.51*$t)+0.4*[Math]::Sin(2*[Math]::PI*1975.53*$t))*$envelope
  $pcm.Write([int16]($value*3800))
}
$data=$samples.ToArray(); $pcm.Dispose(); $samples.Dispose()
$wav=New-Object IO.BinaryWriter([IO.File]::Create((Join-Path $assets 'audio/click.wav')))
$wav.Write([Text.Encoding]::ASCII.GetBytes('RIFF')); $wav.Write([uint32](36+$data.Length)); $wav.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt ')); $wav.Write([uint32]16); $wav.Write([uint16]1); $wav.Write([uint16]1); $wav.Write([uint32]$rate); $wav.Write([uint32]($rate*2)); $wav.Write([uint16]2); $wav.Write([uint16]16); $wav.Write([Text.Encoding]::ASCII.GetBytes('data')); $wav.Write([uint32]$data.Length); $wav.Write($data); $wav.Dispose()
